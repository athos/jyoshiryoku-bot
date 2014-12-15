(ns jyoshiryoku-bot.core
  (:import [twitter4j TwitterFactory Twitter Paging])
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [jyoshiryoku-bot.kaiseki :as kaiseki])
  (:gen-class))

(defn make-twitter []
  (.getInstance (TwitterFactory.)))

(defn tweet [twitter message]
  (.updateStatus twitter message))

(defn mentions-timeline [twitter]
  (lazy-seq (.getMentionsTimeline twitter)))

(defn latest-mention [twitter] (first (mentions-timeline twitter)))

(defn mention-info [twitter]
  (let [mention (latest-mention twitter)
        mentionUser (.getScreenName (.getUser mention))
        mentionText (str/replace (.getText mention) #"(@.*?\s)+" "")
        mentionId (.getId mention)]
    {:userName mentionUser :text mentionText :id mentionId}))

(defn select-word [twitter]
  (kaiseki/token-word
   (first (kaiseki/tokenize (:text (mention-info twitter))))))

(def paging
 (Paging. (int 1) (int 50)))

(defn my-tweets [twitter]
  (map #(.getText %1) (.getUserTimeline twitter paging)))

(defn -main []
  (let [twitter (make-twitter)]
    (with-open [fout (io/writer "tweet.txt" :append true)]
      (.write fout (apply pr-str (my-tweets twitter))))
    (let [words (kaiseki/load-text "tweet.txt")]
      (loop [old (mention-info twitter)]
        (let [new (mention-info twitter)]
          (when-not (= old new)
            (let [sentence (kaiseki/create-sentence words (select-word twitter))
                  message (format ".@%s %s" (:userName new) sentence)]
              (tweet twitter message)))
          (Thread/sleep (* 1000 60 2))
          (recur new))))))
