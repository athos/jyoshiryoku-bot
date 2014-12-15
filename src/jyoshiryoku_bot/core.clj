(ns jyoshiryoku-bot.core
  (:import [twitter4j TwitterFactory Twitter Status Paging]
           [twitter4j.api TimelinesResources TweetsResources])
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [jyoshiryoku-bot.kaiseki :as kaiseki])
  (:gen-class))

(defn make-twitter []
  (.getInstance (TwitterFactory.)))

(defn tweet [^TweetsResources twitter ^String message]
  (.updateStatus twitter message))

(def ^Paging paging (Paging. (int 1) (int 50)))

(defn user-timeline [^TimelinesResources twitter]
  (.getUserTimeline twitter paging))

(defn mentions-timeline [^TimelinesResources twitter]
  (.getMentionsTimeline twitter))

(defn mention->map [^Status mention]
  (let [user (.getScreenName (.getUser mention))
        text (str/replace (.getText mention) #"(@.*?\s)+" "")
        id (.getId mention)]
    {:user user :text text :id id}))

(defn latest-mention [twitter]
  (mention->map (first (mentions-timeline twitter))))

(defn my-tweets [twitter]
  (map #(.getText ^Status %1) (user-timeline twitter)))

(defn select-word [sentence]
  (kaiseki/token-word (first (kaiseki/tokenize sentence))))

(defn main-loop [twitter words]
  (loop [old (latest-mention twitter)]
    (let [new (latest-mention twitter)]
      (when-not (= old new)
        (let [sentence (kaiseki/create-sentence words (select-word (:text new)))
              message (format ".@%s %s" (:user new) sentence)]
          (tweet twitter message)))
      (Thread/sleep (* 1000 60 2))
      (recur new))))

(defn -main []
  (let [twitter (make-twitter)
        tweets (my-tweets twitter)
        _ (spit "tweet.txt" (str/join \newline tweets) :append true)
        words (kaiseki/load-text "tweet.txt")]
    (main-loop twitter words)))
