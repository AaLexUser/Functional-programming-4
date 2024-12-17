(ns query
  (:require [common :as c]
            [clojure.string :as str])
  (:import [dev.langchain4j.model.ollama OllamaChatModel]
           [dev.langchain4j.store.embedding.pgvector PgVectorEmbeddingStore]
           [dev.langchain4j.service AiServices]
           [dev.langchain4j.rag.content.retriever EmbeddingStoreContentRetriever]
           [dev.langchain4j.model.chat ChatLanguageModel]
           [dev.langchain4j.model.input PromptTemplate]
           [dev.langchain4j.rag.query Query]
           [dev.langchain4j.data.message UserMessage]))

(def prompt-template
  "Answer the question based only on the following context:

{{context}}

---

Answer the question based on the above context: {{question}}
Output format: markdown")

(defprotocol Assistant
  (chat [this message]))

(defn get-chat-model []
  (-> (OllamaChatModel/builder)
      (.baseUrl c/llm-base-url)
      (.modelName "llama3.1")  ; matching Python implementation
      (.temperature 0.7)
      .build))

(defn create-content-retriever []
  (-> (EmbeddingStoreContentRetriever/builder)
      (.embeddingStore (c/get-embedding-store))
      (.embeddingModel (c/get-embedding-model))
      (.maxResults (int 5))
      .build))

(defn create-assistant []
  (-> (AiServices/builder Assistant)
      (.chatLanguageModel (get-chat-model))
      (.contentRetriever (create-content-retriever))
      .build))

(defn format-sources [results]
  (map (fn [result]
         {:id (-> result (get ".metadata"))
          :content (.textSegment result)})
       results))

(defn query-rag [query-text]
  (let [retriever (create-content-retriever)
        query (Query/from (str/trim query-text))
        results (.retrieve retriever query)
        context (str/join "\n\n---\n\n" (map #(.textSegment %) results))
        prompt (-> (PromptTemplate/from prompt-template)
                   (.apply (java.util.HashMap. {"context" context
                                              "question" query-text})))
        _ (println "PROMPT:\n" prompt)
        model (get-chat-model)
        response (.generate model [(UserMessage/from (.text prompt))])
        sources (format-sources results)]
    (println (format "Response: %s\nSources: %s" (.text (.content response)) sources))
    {:response (.text (.content response))
     :sources sources}))

(defn -main [& args]
  (when-let [query-text (first args)]
    (query-rag query-text)))

(comment
  (let [retriever (create-content-retriever)
        query (Query/from "Цель курса")]
    (.retrieve retriever query))
  )