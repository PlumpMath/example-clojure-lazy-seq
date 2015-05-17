(ns examples-lazy-seq.core
  (:use [clojure.pprint :only [pprint]]))

(defn -main
  "I don't do a whole lot."
  [& args]

  ;; TODO:
  ;;  Lazy sequence 설명.

  (def random (fn random []
                (lazy-seq
                  (cons (rand-int 100) (random)))))
  ;;=> #'examples-lazy-seq.core/random

  ;; 위 "random" 함수는 조건없이 재귀호출을 한다.
  ;; 즉, 이 함수를 호출하면 끝나지 않거나 스택을 모두 소진하여 예외를 발생시킨다.
  ;; 하지만, 아래와 같이 "random" 함수를 호출해도 위에서 언급한 결과는 발생하지 않는다.
  ;; 이는 lazy-seq가 자신의 몸통의 표현식을 지연평가(lazy evaluation) 시키기 때문이다.

  (def def-test (println "invoked!!"))
  ;;=> invoked!!
  ;;=> #'examples-lazy-seq.core/def-test
  (def random-result
    "REPL에서 'random' 함수를 호출하면, 그 결과를 보여주기 위해 바로 평가하기 때문에 -
     random-result 변수에 'random' 함수의 결과를 담는 방식으로 'random' 함수를 호출하였다."
    (random))
  ;;=> #'examples-lazy-seq.core/random-result

  ;; 아래와 같이 lazy-seq 매크로를 풀어보면 몸통을 lambda로 감싸 LazySeq 클래스의 파라메터로 전달한다.
  ;; 즉, lazy-seq는 평가를 지연시키기 위해 몸통의 표현식을 lambda로 감싸고-
  ;; LazySeq 클래스에게 이 lambda 함수의 실행을 위임한다.

  (macroexpand '(lazy-seq (cons (rand-int 100) (random))))
  ;;=> (new clojure.lang.LazySeq (fn* [] (cons (rand-int 100) (random))))

  ;; LazySeq 클래스는 주어진 lambda를 보관하고 있다가 LazySeq.seq 메서드가 호출되면 수행시킨다.
  ;; 아래와 같이 seq 함수로 LazySeq.seq 메서드를 간접적으로 호출하면 lazy-seq의 몸통이 실행된다.

  (def test-lazy-eval (lazy-seq (do (println "evaluate") nil)))
  ;;=> #'examples-lazy-seq.core/test-lazy-eval
  (seq test-lazy-eval)
  ;;=> evaluate
  ;;=> nil

  ;; 여태 까지의 내용을 요약하면,

  ;; lazy-seq 매크로는 자신의 몸통을 lambda로 감싸 LazySeq 클래스를 생성할 때 파라메터로 넘긴다.
  ;; 이 lambda 함수는 LazySeq.seq 메서드가 호출될 때 평가되고,
  ;; seq 메서드는 그 결과 값을 반환한다.

  ;; 그리고, LazySeq.seq 메서드는 ISeq 타입을 반환하므로
  ;; lazy-seq의 몸통의 결과는 ISeq 타입으로 변환 가능해야 한다.
  ;; 참고로 변경가능한 타입들은 RT.seq와 RT.seqFrom 메서드에 정의되어 있다.

  (seq (lazy-seq '(1 2 3)))
  ;;=> (1 2 3)
  (seq (lazy-seq nil))
  ;;=> nil
  (seq (lazy-seq 1))
  ;;=> IllegalArgumentException Don't know how to create ISeq from: java.lang.Long
  ;;=>  clojure.lang.RT.seqFrom (RT.java:505)


  #_(fn random []
      (lazy-seq
        (cons (rand-int 100) (random))))

  ;; "random" 함수를 다시 살펴보면,
  ;; 끝나는 조건이 없는 재귀 함수이기 때문에 평가되면 무한히 호출될 것 같지만,
  ;; 아래와 같이 평가(호출)되어도 무한히 호출되지 않는다.

  (first (random))
  ;;=> 81

  ;; first 함수가 어떤 마법을 부린 걸까? 파해쳐보자!
  ;; 클로저 RT.java 파일에 정의된 first 함수를 따라가면,
  ;; "random" 함수의 결과의 ISeq.first 머세드를 호출한다.

  ;; "random" 함수, 즉 lazy-seq 매크로는 LazySeq 클래스를 반환하므로
  ;; first 함수는 LazySeq.first 메서드를 호출한다.

  ;; LazySeq.first 메서드는 lazy-seq의 몸통을 평가하여 결과의 ISeq.first 메서드를 호출한다.
  ;; "random" 함수의 경에는 cons 함수를 사용하기 때문에 Cons 클래스를 반환한다.
  ;; 결국, Cons.first 메서드가 호출된다.

  ;; 먼저, cons 함수는 `임의의 값'과 `시퀀스'를 받아 Cons 클래스를 반환한다.
  ;; Cons.java 파일에 정의된 Cons.first 메서드는 전달받은 `임의의 값'을 반환하기만 하고,
  ;; Cons 클래스를 생성할때 같이 전달받은 `시퀀스'에는 접근하지도 않는다.

  ;; 다시말해, "random" 함수가 평가되었을 때 무한히 호출되지 않은 이유는
  ;; first 함수 때문이 아니라 lazy-seq와 cons 함수 때문이다.

  ;; 아래와 같이 cons 함수는 전달받은 `시퀀스("random" 함수의 경우 LazySeq)'를 평가하지 않고,
  ;; lazy-seq 매크로는 자신의 몸통의 평가를 지연시키기 때문에 "random" 함수는 무한히 호출되지 않았다.
  ;; 즉, LazySeq 클래스가 -지연된- 값이고, Cons 클래스는 이 값들이 바로 평가되지 않는 방법으로 연결한다.

  (first  (cons (rand-int 100) (lazy-seq (do (println "evaluate") nil))))
  ;;=> 29
  (second (cons (rand-int 100) (lazy-seq (do (println "evaluate") nil))))
  ;;=> evaluate
  ;;=> nil
  )
