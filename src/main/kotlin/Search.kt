package com.example.personvectorsearch

import java.util.Scanner
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.KnnVectorQuery
import org.apache.lucene.store.FSDirectory
import java.nio.file.Files
import java.nio.file.Paths
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

@Serializable
data class PersonSearchResult(
    val rank: Int,
    val id: String,
    val bio: String,
    val category: String,
    val resumeHtml: String,
    val score: Float
)

class EmbeddingService {
    private val client = HttpClient()

    suspend fun textToVector(text: String, retries: Int = 3): FloatArray {
        var attempt = 0
        while (attempt < retries) {
            try {
                val startTime = System.currentTimeMillis()
                println("Sending request to embedding server for: '$text' (attempt ${attempt + 1})")

                val response: HttpResponse = client.post("http://127.0.0.1:5000/embed") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"text":"${text.replace("\"", "\\\"")}"}""")
                }

                val embeddingJson = response.bodyAsText()
                println("Got JSON response (${embeddingJson.length} chars): ${embeddingJson.take(50)}...")

                if (response.status != HttpStatusCode.OK) {
                    println("Embedding server error: $embeddingJson")
                    throw RuntimeException("Embedding server returned ${response.status}")
                }

                val embedding: List<Float> = Json.decodeFromString(embeddingJson)

                if (embedding.size != 384) {
                    throw RuntimeException("Embedding has incorrect dimensions: ${embedding.size}")
                }

                println("Embedding parsed successfully in ${System.currentTimeMillis() - startTime}ms")
                return embedding.toFloatArray()
            } catch (e: Exception) {
                println("Error converting text to vector: ${e.message}")
                attempt++
                if (attempt >= retries) {
                    throw RuntimeException("Failed to get embedding after $retries attempts", e)
                } else {
                    println("Retrying in 1s...")
                    kotlinx.coroutines.delay(1000)
                }
            }
        }
        throw RuntimeException("Should not reach here")
    }

    fun close() {
        client.close()
    }
}

fun main() {
    runBlocking {
        val indexPath = Paths.get("lucene_index")
        if (!Files.exists(indexPath)) {
            println("Error: Lucene index not found at: $indexPath")
            println("Please run PersonIndexer.kt first to create the index")
            return@runBlocking
        }

        FSDirectory.open(indexPath).use { directory ->
            DirectoryReader.open(directory).use { reader ->
                val searcher = IndexSearcher(reader)

                // Verify index contents
                println("Index contains ${reader.numDocs()} documents")
                if (reader.numDocs() == 0) {
                    println("Error: Lucene index is empty")
                    return@runBlocking
                }

                // Initialize embedding service
                val embeddingService = EmbeddingService()

                try {
                    // Get user query - hardcoded for testing
                    val userQuery = "lawyer"  // Sample input
                    println("Search query: '$userQuery'")
                    println("Converting '$userQuery' to vector...")

                    // Convert user text to vector
                    val queryVector = embeddingService.textToVector(userQuery)
                    println("Vector generated successfully (dimension: ${queryVector.size})")

                    // Perform KNN search
                    val topN = 30
                    println("Performing KNN search for top $topN results...")
                    val topDocs = searcher.search(KnnVectorQuery("embedding", queryVector, topN), topN)
                    println("KNN search completed, found ${topDocs.scoreDocs.size} results")

                    println("\nTop $topN people for query: '$userQuery'")
                    println("=".repeat(80))

                    if (topDocs.scoreDocs.isEmpty()) {
                        println("No matching people found")
                    } else {
                        val results = mutableListOf<PersonSearchResult>()

                        topDocs.scoreDocs.forEachIndexed { index, sd ->
                            val doc = searcher.doc(sd.doc)
                            val id = doc.get("id") ?: "Unknown ID"
                            val bio = doc.get("bio") ?: "No bio available"
                            val category = doc.get("category") ?: "Unknown category"
                            val resumeHtml = doc.get("resume_html") ?: "No resume available"

                            val result = PersonSearchResult(
                                rank = index + 1,
                                id = id,
                                bio = bio,
                                category = category,
                                resumeHtml = resumeHtml,
                                score = sd.score
                            )
                            results.add(result)

                            println("${index + 1}. ID: $id")
                            println("   Score: ${String.format("%.4f", sd.score)}")
                            println("   Category: $category")
                            println("   Bio: ${bio.take(100)}${if (bio.length > 100) "..." else ""}")
                            println("   Resume: ${if (resumeHtml.isNotBlank()) "Available (${resumeHtml.length} chars)" else "Not available"}")
                            println()
                        }

                        // Auto-save results to JSON (no user input needed for testing)
                        val json = Json { prettyPrint = true }
                        val resultsJson = json.encodeToString<List<PersonSearchResult>>(results)
                        val filename = "search_results_${System.currentTimeMillis()}.json"
                        Files.write(Paths.get(filename), resultsJson.toByteArray())
                        println("Results saved to: $filename")
                    }
                } catch (e: Exception) {
                    println("Error during search: ${e.message}")
                    println("Make sure:")
                    println("1. Flask server is running (python embed_server.py)")
                    println("2. sentence-transformers and flask are installed: pip install sentence-transformers flask")
                    println("3. Lucene index contains valid embeddings (run PersonIndexer.kt first)")
                    println("4. embeddings.json matches people_with_index.json")
                    e.printStackTrace()
                } finally {
                    embeddingService.close()
                }
            }
        }
    }
}