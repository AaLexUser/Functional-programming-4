(ns common
  (:import dev.langchain4j.model.ollama.OllamaEmbeddingModel
           dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore))

(def llm-base-url (or (System/getenv "OLLAMA_HOST") "http://localhost:11434"))
(def llm-embedding-model (or (System/getenv "LLM_EMBEDDING_MODEL") "nomic-embed-text"))
(def db-host (or (System/getenv "DB_HOST") "localhost"))
(def db-port (Integer/parseInt (or (System/getenv "DB_PORT") "5432")))
(def db-name (or (System/getenv "DB_NAME") "vectordb"))
(def db-user (or (System/getenv "DB_USER") "postgres"))
(def db-password (or (System/getenv "DB_PASSWORD") "postgres"))

(defn get-embedding-model []
  (-> (OllamaEmbeddingModel/builder)
      (.baseUrl llm-base-url)
      (.modelName llm-embedding-model)
      .build))

(defn get-embedding-store []
  (-> (PgVectorEmbeddingStore/builder)
      (.host db-host)
      (.port db-port)
      (.database db-name)
      (.user db-user)
      (.password db-password)
      (.table "embeddings")
      (.dimension (.dimension (get-embedding-model)))
      (.build)))