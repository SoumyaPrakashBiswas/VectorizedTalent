import pandas as pd
import json
from sentence_transformers import SentenceTransformer
import numpy as np

def main():
    # Load the sentence transformer model
    print("Loading sentence transformer model...")
    model = SentenceTransformer('all-MiniLM-L6-v2')
    
    csv_file = 'people_data.csv'  
    try:
        df = pd.read_csv(csv_file)
        print(f"Loaded {len(df)} records from {csv_file}")
    except FileNotFoundError:
        print(f"Error: CSV file '{csv_file}' not found")
        return
    except Exception as e:
        print(f"Error reading CSV: {e}")
        return
    
    required_columns = ['ID', 'Bio', 'Category']
    missing_columns = [col for col in required_columns if col not in df.columns]
    if missing_columns:
        print(f"Error: Missing required columns: {missing_columns}")
        print(f"Available columns: {list(df.columns)}")
        return
    
    # Fill NaN values with empty strings
    df = df.fillna('')
    
    # Create combined text for embeddings (Bio + Category)
    print("Creating combined text for embeddings...")
    combined_texts = []
    people_data = []
    
    for idx, row in df.iterrows():
        # Combine bio and category for embedding
        combined_text = f"{row['Bio']} {row['Category']}"
        combined_texts.append(combined_text)
        
        # Prepare person data
        person_data = {
            'ID': str(row['ID']),
            'Bio': str(row['Bio']),
            'Resume_html': str(row.get('Resume_html', '')),
            'Category': str(row['Category']),
            'embedding_index': idx
        }
        people_data.append(person_data)
        
        if (idx + 1) % 100 == 0:
            print(f"Processed {idx + 1}/{len(df)} records...")
    
    # Generate embeddings for all combined texts
    print("Generating embeddings...")
    embeddings = model.encode(combined_texts, show_progress_bar=True)
    
    # Convert to list format for JSON serialization
    embeddings_list = [embedding.tolist() for embedding in embeddings]
    
    # Verify embedding dimensions
    expected_dim = 384
    if embeddings_list and len(embeddings_list[0]) != expected_dim:
        print(f"Warning: Embedding dimension is {len(embeddings_list[0])}, expected {expected_dim}")
    else:
        print(f"✓ All embeddings have correct dimension: {expected_dim}")
    
    # Save people data with embedding indices
    people_output_file = 'people_with_index.json'
    with open(people_output_file, 'w', encoding='utf-8') as f:
        json.dump(people_data, f, indent=2, ensure_ascii=False)
    print(f"✓ Saved people data to {people_output_file}")
    
    # Save embeddings
    embeddings_output_file = 'embeddings.json'
    with open(embeddings_output_file, 'w', encoding='utf-8') as f:
        json.dump(embeddings_list, f, indent=2)
    print(f"✓ Saved {len(embeddings_list)} embeddings to {embeddings_output_file}")
    
    print(f"\nProcessing complete!")
    print(f"- Total records processed: {len(people_data)}")
    print(f"- Embeddings generated: {len(embeddings_list)}")
    print(f"- Files created: {people_output_file}, {embeddings_output_file}")
    print(f"\nNext steps:")
    print(f"1. Run PersonIndexer.kt to create the Lucene index")
    print(f"2. Start embed_server.py for user query embeddings")
    print(f"3. Run PersonSearcher.kt to perform searches")

if __name__ == "__main__":
    main()