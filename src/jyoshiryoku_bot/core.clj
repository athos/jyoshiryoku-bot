(ns jyoshiryoku-bot.core
  (:import [twitter4j TwitterFactory Twitter Paging])
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [jyoshiryoku-bot.kaiseki :as kaiseki])
  (:gen-class))

(defn mytwitter []
  (.getInstance (TwitterFactory.)))

(defn tweettimeline [twitter message]
  (.updateStatus twitter message))

(defn mymention [twitter]
  (lazy-seq (.getMentionsTimeline twitter)))

(defn resentmention [twitter] (first (mymention twitter)))

(defn mentionInfo [twitter]
  (let [mention (resentmention twitter)
        mentionUser (.getScreenName (.getUser mention))
        mentionText (str/replace (.getText mention) #"(@.*?\s)+" "")
        mentionId (.getId mention)]
    {:userName mentionUser :text mentionText :id mentionId}))

(defn searchword [twitter]
  (kaiseki/token-word
   (first (kaiseki/tokenize (:text (mentionInfo twitter))))))

(def paging
 (Paging. (int 1) (int 50)))

(defn getmytweet [twitter]
  (map #(.getText %1) (.getUserTimeline twitter paging)))

(defn -main []
  (let [twitter (mytwitter)]
    (with-open [fout (io/writer "tweet.txt" :append true)]
      (.write fout (apply pr-str (getmytweet twitter))))
    (let [words (kaiseki/load-text "tweet.txt")]
      (loop [old (mentionInfo twitter)]
        (let [new (mentionInfo twitter)]
          (when-not (= old new)
            (let [sentence (kaiseki/create-sentence words (searchword twitter))
                  message (format ".@%s %s" (:userName new) sentence)]
              (tweettimeline twitter message)))
          (Thread/sleep (* 1000 60 2))
          (recur new))))))
