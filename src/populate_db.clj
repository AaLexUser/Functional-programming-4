(ns populate-db
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
           dev.langchain4j.model.ollama.OllamaEmbeddingModel
           dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore
           dev.langchain4j.store.embedding.EmbeddingStoreIngestor
           dev.langchain4j.data.document.splitter.DocumentSplitters
           dev.langchain4j.data.document.parser.TextDocumentParser
           java.nio.file.FileSystems))

(def data-path "data")
(def chunk-size 500)
(def chunk-overlap 200)

(def llm-base-url (or (System/getenv "LLM_HOST") "http://localhost:11434"))
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


(defn get-existing-ids [embedding-store]
  (let [existing-items (.findAll embedding-store)]
    (into #{} (map #(.id %) existing-items))))

(defn clear-database []
  (println "✨ Clearing Database")
  (let [embedding-store (get-embedding-store)]
    (.deleteAll embedding-store)))

(defn load-documents []
  (let [path-matcher (.getPathMatcher (FileSystems/getDefault) "glob:*.md")
        parser (TextDocumentParser.)
        documents (FileSystemDocumentLoader/loadDocuments data-path path-matcher parser)]
    (println (str "Loaded " (count documents) " documents."))
    documents))

(defn split-text [documents]
  (let [splitter (DocumentSplitters/recursive chunk-size chunk-overlap)
        chunks (reduce (fn [acc doc]
                         (concat acc (.split splitter doc)))
                       []
                       documents)]
    (println (str "Split " (count documents) " documents into " (count chunks) " chunks."))
    chunks))

(defn calculate-chunk-ids [chunks]
  (map (fn [chunk]
         (let [metadata (.metadata chunk)
               source (.get metadata "source")
               index (.get metadata "start_index")
               chunk-id (str source ":" index)]
           (.put metadata "id" chunk-id)
           chunk))
       chunks))

(defn ingest-documents [documents]
  (let [chunks (split-text documents)
        chunks-with-ids (calculate-chunk-ids chunks)
        embedding-model (get-embedding-model)
        embedding-store (get-embedding-store)
        existing-ids (get-existing-ids embedding-store)
        new-chunks (filter #(not (contains? existing-ids (.get (.metadata %) "id"))) chunks-with-ids)]
    (println (str "Number of existing documents in DB: " (count existing-ids)))
    (if (seq new-chunks)
      (do
        (println (str "👉 Adding new documents: " (count new-chunks)))
        (let [ingestor (-> (EmbeddingStoreIngestor/builder)
                           (.embeddingModel embedding-model)
                           (.embeddingStore embedding-store)
                           .build)]
          (.ingest ingestor new-chunks)))
      (println "✅ No new documents to add"))))

(defn -main [& args]
  (let [cli-options [["-r" "--reset" "Reset the database"]]
        {:keys [options]} (parse-opts args cli-options)]
    (when (:reset options)
      (clear-database))
    (let [documents (load-documents)]
      (ingest-documents documents)
      (println "✨ Done populating database"))))