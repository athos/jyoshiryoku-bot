(ns jyoshiryoku-bot.core
  (:import [twitter4j TwitterFactory Twitter Paging])
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [jyoshiryoku-bot.kaiseki :as kaiseki])
  (:gen-class))

(defn mytwitter []
  (.getInstance (TwitterFactory.)))

(defn tweettimeline [message]
  (let [twitter (mytwitter)]
    (.updateStatus twitter message)))

(defn mymention []
  (lazy-seq (.getMentionsTimeline (mytwitter))))

(defn resentmention [] (first (mymention)))

(defn mentionInfo []
  (let [mention (resentmention)
        mentionUser (.getScreenName (.getUser mention))
        mentionText (str/replace (.getText mention) #"(@.*?\s)+" "")
        mentionId (.getId mention)]
    {:userName mentionUser :text mentionText :id mentionId}))

(defn searchword []
  (kaiseki/token-word
   (first (kaiseki/tokenize (:text (mentionInfo))))))

(def paging
 (Paging. (int 1) (int 50)))

(defn getmytweet []
  (let [twitter (mytwitter)]
     (map #(.getText %1) (.getUserTimeline twitter paging))))

(defn -main []
  (with-open [fout (io/writer "tweet.txt" :append true)]
    (.write fout (apply pr-str (getmytweet))))
  (let [words (kaiseki/load-text "tweet.txt")]
    (loop [old (mentionInfo)]
      (let [new (mentionInfo)]
        (when-not (= old new)
          (let [sentence (kaiseki/create-sentence words (searchword))
                message (format ".@%s %s" (:userName new) sentence)]
            (tweettimeline message)))
        (Thread/sleep (* 1000 60 2))
        (recur new)))))
