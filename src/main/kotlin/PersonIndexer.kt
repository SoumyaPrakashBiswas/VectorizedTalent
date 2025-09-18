package com.example.personvectorsearch

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.*
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.VectorSimilarityFunction
import org.apache.lucene.store.FSDirectory
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Paths
import com.example.personvectorsearch.Person

fun main() {
    val indexPath = Paths.get("lucene_index")
    if (Files.exists(indexPath)) {
        println("Clearing existing index at: $indexPath")
        Files.walk(indexPath).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
    }

    val analyzer = StandardAnalyzer()
    val directory = FSDirectory.open(indexPath)
    val config = IndexWriterConfig(analyzer)
    val writer = IndexWriter(directory, config)

    // Load people
    val json = Json { ignoreUnknownKeys = true }
    val peopleJsonPath = Paths.get("people_with_index.json")
    if (!Files.exists(peopleJsonPath)) {
        println("Error: People JSON file not found at: $peopleJsonPath")
        println("Please run generate_embeddings.py first to generate the JSON files")
        writer.close()
        return
    }
    val peopleJson = peopleJsonPath.toFile().readText()
    val people: List<Person> = try {
        json.decodeFromString(peopleJson)
    } catch (e: Exception) {
        println("Error parsing people JSON: ${e.message}")
        writer.close()
        return
    }

    // Load embeddings
    val embeddingsJsonPath = Paths.get("embeddings.json")
    if (!Files.exists(embeddingsJsonPath)) {
        println("Error: Embeddings JSON file not found at: $embeddingsJsonPath")
        println("Please run generate_embeddings.py first to generate the JSON files")
        writer.close()
        return
    }
    val embeddingsJson = embeddingsJsonPath.toFile().readText()
    val embeddings: List<List<Float>> = try {
        json.decodeFromString(embeddingsJson)
    } catch (e: Exception) {
        println("Error parsing embeddings JSON: ${e.message}")
        writer.close()
        return
    }

    println("Loaded ${people.size} people and ${embeddings.size} embeddings")

    // Validate embeddings
    people.forEachIndexed { index, person ->
        if (person.embedding_index !in embeddings.indices) {
            println("Warning: Invalid embedding_index ${person.embedding_index} for person ID '${person.ID}' at index $index")
        } else if (embeddings[person.embedding_index].size != 384) {
            println("Warning: Embedding for person ID '${person.ID}' has incorrect dimensions: ${embeddings[person.embedding_index].size}, expected 384")
        }
    }

    // Index people
    var indexedDocs = 0
    people.forEach { person ->
        if (person.embedding_index in embeddings.indices) {
            val doc = Document()

            // Add searchable and stored fields
            doc.add(TextField("id", person.ID, Field.Store.YES))
            doc.add(TextField("bio", person.Bio, Field.Store.YES))
            doc.add(TextField("category", person.Category, Field.Store.YES))
            doc.add(StoredField("resume_html", person.Resume_html)) // Store but don't index for search

            // Add vector field for similarity search
            val vec = embeddings[person.embedding_index].toFloatArray()
            val knnField = KnnFloatVectorField("embedding", vec, VectorSimilarityFunction.DOT_PRODUCT)
            doc.add(knnField)

            writer.addDocument(doc)
            indexedDocs++

            if (indexedDocs % 100 == 0) {
                println("Indexed $indexedDocs documents...")
            }
        }
    }

    writer.commit()
    writer.close()
    println("✓ Indexing complete! Indexed $indexedDocs documents with vectors.")
    println("✓ Index created at: $indexPath")
}