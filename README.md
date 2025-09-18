Resume Vector Search with Lucene-

This project implements a vector-based search engine for resumes (CVs) using Lucene’s KNN search.
It allows storing resumes as embeddings in a Lucene index, and then querying them with natural language search by converting user queries into embeddings and retrieving the top-k most relevant results.

🚀 Process:

📄 Resume Structuring – Extract and preprocess CV data.

🧠 Embedding Generation – Convert resumes and queries into dense vector embeddings using an embedding model.

📦 Lucene Indexing – Store embeddings in a Lucene index with vector fields.

🔍 Vector Search (kNN) – Retrieve the most similar resumes for a given query.

⚡ Efficient Retrieval – Uses Lucene’s native vector search (no hybrid text+vector needed).

⚙️ Workflow

Parse & preprocess CVs – Convert resumes into structured text.

Generate embeddings – Use an embedding model to transform text into vectors.

Index embeddings in Lucene – Store vectors in Lucene with metadata.

Query embedding generation – Convert user query into an embedding at runtime.

Vector search – Perform k-nearest neighbor (kNN) search against Lucene’s vector field.

Return results – Top-k most relevant resumes are returned.

🛠️ Tech Stack

Kotlin /   Python   – Core implementation.

Apache Lucene – For indexing and kNN vector search.

Embedding Model – Any model (e.g., OpenAI, HuggingFace, or custom).

📊 Example Use Case

Input: "Find resumes with experience in machine learning and AWS"

System generates query embedding → runs kNN search → returns top-k resumes most relevant to the query.
