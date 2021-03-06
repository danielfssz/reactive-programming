package academy.devdojo.reactive.test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

@Slf4j
public class OperatorsTest {

	@BeforeAll
	public static void setup() {
		BlockHound.install();
	}

	@Test
	public void subscribeOnSimple() {
		Flux<Integer> flux = Flux.range(1, 4)
				.map(i -> {
					log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
					return i;
				})
				.subscribeOn(Schedulers.single())
				.map(i -> {
					log.info("Map 2 - Number {} on Thread {}", i, Thread.currentThread().getName());
					return i;
				});

		StepVerifier.create(flux)
				.expectSubscription()
				.expectNext(1, 2, 3, 4)
				.verifyComplete()
		;
	}

	@Test
	public void publishOnSimple() {
		Flux<Integer> flux = Flux.range(1, 4)
				.map(i -> {
					log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
					return i;
				})
				.publishOn(Schedulers.single())
				.map(i -> {
					log.info("Map 2 - Number {} on Thread {}", i, Thread.currentThread().getName());
					return i;
				});

		flux.subscribe();
		flux.subscribe();

		StepVerifier.create(flux)
				.expectSubscription()
				.expectNext(1, 2, 3, 4)
				.verifyComplete()
		;
	}

	@Test
	public void multipleSubscribeOnSimple() {
		Flux<Integer> flux = Flux.range(1, 4)
				.subscribeOn(Schedulers.boundedElastic())
				.map(i -> {
					log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
					return i;
				})
				.subscribeOn(Schedulers.single())
				.map(i -> {
					log.info("Map 2 - Number {} on Thread {}", i, Thread.currentThread().getName());
					return i;
				});

		flux.subscribe();
		flux.subscribe();

		StepVerifier.create(flux)
				.expectSubscription()
				.expectNext(1, 2, 3, 4)
				.verifyComplete()
		;
	}

	@Test
	public void multiplePublishOnSimple() {
		Flux<Integer> flux = Flux.range(1, 4)
				.publishOn(Schedulers.single())
				.map(i -> {
					log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
					return i;
				})
				.publishOn(Schedulers.boundedElastic())
				.map(i -> {
					log.info("Map 2 - Number {} on Thread {}", i, Thread.currentThread().getName());
					return i;
				});

		StepVerifier.create(flux)
				.expectSubscription()
				.expectNext(1, 2, 3, 4)
				.verifyComplete()
		;
	}

	@Test
	public void publishAndSubscribeOnSimple() {
		Flux<Integer> flux = Flux.range(1, 4)
				.publishOn(Schedulers.single())
				.map(i -> {
					log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
					return i;
				})
				.subscribeOn(Schedulers.boundedElastic())
				.map(i -> {
					log.info("Map 2 - Number {} on Thread {}", i, Thread.currentThread().getName());
					return i;
				});

		StepVerifier.create(flux)
				.expectSubscription()
				.expectNext(1, 2, 3, 4)
				.verifyComplete()
		;
	}

	@Test
	public void subscribeAndPublishOnSimple() {
		Flux<Integer> flux = Flux.range(1, 4)
				.subscribeOn(Schedulers.single())
				.map(i -> {
					log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
					return i;
				})
				.publishOn(Schedulers.boundedElastic())
				.map(i -> {
					log.info("Map 2 - Number {} on Thread {}", i, Thread.currentThread().getName());
					return i;
				});

		StepVerifier.create(flux)
				.expectSubscription()
				.expectNext(1, 2, 3, 4)
				.verifyComplete()
		;
	}

	@Test
	public void subscribeOnIO() throws Exception {
		// executa a chamada que esta bloqueando a thread em backgroud
		Mono<List<String>> list = Mono.fromCallable(() -> Files.readAllLines(Paths.get("text-file")))
				.log()
				.subscribeOn(Schedulers.boundedElastic());

		list.subscribe(i -> log.info("{}", i));

		Thread.sleep(200);

		StepVerifier.create(list)
				.expectSubscription()
				.thenConsumeWhile(l -> {
					Assertions.assertFalse(l.isEmpty());
					log.info("Size {}", l.size());
					return true;
				})
				.verifyComplete();
	}

	@Test
	public void switchIfEmptyOperator() {
		Flux<Object> flux = emptyFlux()
				.switchIfEmpty(Flux.just("not empty anymore"))
				.log();

		StepVerifier.create(flux)
				.expectSubscription()
				.expectNext("not empty anymore")
				.expectComplete()
				.verify();
	}

	private Flux<Object> emptyFlux() {
		return Flux.empty();
	}

	@Test
	public void deferOperator() throws Exception { //adia a execucao do que vc tem dentro do operador
		Mono<Long> just = Mono.just(System.currentTimeMillis());
		Mono<Long> defer = Mono.defer(() -> Mono.just(System.currentTimeMillis()));

		just.subscribe(l -> log.info("just time {}", l));
		Thread.sleep(100);
		just.subscribe(l -> log.info("just time {}", l));
		Thread.sleep(100);
		just.subscribe(l -> log.info("just time {}", l));

		defer.subscribe(l -> log.info("defer time {}", l));
		Thread.sleep(100);
		defer.subscribe(l -> log.info("defer time {}", l));
		Thread.sleep(100);
		defer.subscribe(l -> log.info("defer time {}", l));

		AtomicLong atomicLong = new AtomicLong();
		defer.subscribe(atomicLong::set);
		Assertions.assertTrue(atomicLong.get() > 0);

	}

	@Test
	public void concatOperator() {
		Flux<String> flux1 = Flux.just("a", "b");
		Flux<String> flux2 = Flux.just("c", "d");

		Flux<String> fluxConcat = Flux.concat(flux1, flux2).log();

		StepVerifier
				.create(fluxConcat)
				.expectSubscription()
				.expectNext("a", "b", "c", "d")
				.expectComplete()
				.verify();

	}

	@Test
	public void concatOperatorError() {
		Flux<String> flux1 = Flux.just("a", "b")
				.map(s -> {
					if (s.equals("b")) {
						throw new IllegalArgumentException("");
					}
					return s;
				});

		Flux<String> flux2 = Flux.just("c", "d");

		Flux<String> fluxConcat = Flux.concatDelayError(flux1, flux2).log();

		StepVerifier
				.create(fluxConcat)
				.expectSubscription()
				.expectNext("a", "c", "d")
				.expectError()
				.verify();
	}

	@Test
	public void concatWithOperator() {
		Flux<String> flux1 = Flux.just("a", "b");
		Flux<String> flux2 = Flux.just("c", "d");

		Flux<String> fluxConcat = flux1.concatWith(flux2).log();

		StepVerifier
				.create(fluxConcat)
				.expectSubscription()
				.expectNext("a", "b", "c", "d")
				.expectComplete()
				.verify();

	}

	@Test
	public void combineLastOperator() {
		Flux<String> flux1 = Flux.just("a", "b");
		Flux<String> flux2 = Flux.just("c", "d");

		Flux.combineLatest(flux1, flux2, (s1, s2) -> s1.toUpperCase() + s2.toUpperCase())
				.log();

		StepVerifier.create(flux1)
				.expectSubscription()
				.expectNext("BC", "BD")
				.expectComplete()
				.verify();
	}

	@Test
	public void mergeOperator() {
		Flux<String> flux1 = Flux.just("a", "b").delayElements(Duration.ofMillis(200));
		Flux<String> flux2 = Flux.just("c", "d");

		Flux<String> merge = Flux.merge(flux1, flux2)
				.log();

		merge.subscribe(log::info);

		StepVerifier
				.create(merge)
				.expectSubscription()
				.expectNext("a", "b", "c", "d")
				.expectComplete()
				.verify()
		;
	}

	@Test
	public void mergeWithOperator() {
		Flux<String> flux1 = Flux.just("a", "b").delayElements(Duration.ofMillis(200));
		Flux<String> flux2 = Flux.just("c", "d");

		Flux<String> merge = flux1.mergeWith(flux2)
				.log();

		merge.subscribe(log::info);

		StepVerifier
				.create(merge)
				.expectSubscription()
				.expectNext("a", "b", "c", "d")
				.expectComplete()
				.verify()
		;
	}

	@Test
	public void mergeSequentialOperator() {
		Flux<String> flux1 = Flux.just("a", "b").delayElements(Duration.ofMillis(200));
		Flux<String> flux2 = Flux.just("c", "d");

		Flux<String> merge = flux1.mergeSequential(flux1, flux2, flux1)
				.delayElements(Duration.ofMillis(200))
				.log();

		StepVerifier
				.create(merge)
				.expectSubscription()
				.expectNext("a", "b", "c", "d", "a", "b")
				.expectComplete()
				.verify()
		;
	}

	@Test
	public void mergeDelayErrorOperator() {
		Flux<String> flux1 = Flux.just("a", "b")
				.map(s -> {
					if (s.equals("b")) {
						throw new IllegalArgumentException("");
					}
					return s;
				})
				.doOnError(t -> log.error("Error"));

		Flux<String> flux2 = Flux.just("c", "d");

		Flux<String> mergeFlux = Flux.mergeDelayError(1, flux1, flux2, flux1)
				.delayElements(Duration.ofMillis(200))
				.log();

		mergeFlux.subscribe(log::info);

//		StepVerifier
//				.create(mergeFlux)
//				.expectSubscription()
//				.expectNext("a", "b", "c", "d", "a", "b")
//				.expectComplete()
//				.verify();
	}

	@Test
	public void flatMapOperator() throws Exception {
		Flux<String> flux = Flux.just("a", "b").delayElements(Duration.ofMillis(200));

		Flux<String> flatFlux = flux
				.map(String::toUpperCase)
				.flatMap(this::findByName)
				.log();

		flatFlux.subscribe(log::info);

		Thread.sleep(500);

		StepVerifier.create(flatFlux)
				.expectSubscription()
				.expectNext("nomeB1", "nomeB2", "nomeA1", "nomeA2")
				.verifyComplete()
		;
	}

	public Flux<String> findByName(String name) {
		return name.equals("A")
				? Flux.just("nomeA1", "nomeA2").delayElements(Duration.ofMillis(100))
				: Flux.just("nomeB1", "nomeB2");
	}

	@Test
	public void flatMapSequentialOperator() throws Exception {
		Flux<String> flux = Flux.just("a", "b").delayElements(Duration.ofMillis(200));

		Flux<String> flatFlux = flux
				.map(String::toUpperCase)
				.flatMapSequential(this::findByName)
				.log();

		flatFlux.subscribe(log::info);

		Thread.sleep(500);

		StepVerifier.create(flatFlux)
				.expectSubscription()
				.expectNext("nomeA1", "nomeA2", "nomeB1", "nomeB2")
				.verifyComplete()
		;
	}

	@Test
	public void zipOperator() {
		Flux<String> titleFlux = Flux.just("La Casa de Papel", "Chicago PD");
		Flux<Integer> episodesFlux = Flux.just(40, 300);

		Flux<Serie> serieFlux = Flux
				.zip(titleFlux, episodesFlux)
				.flatMap(tuple -> Flux.just(new Serie(tuple.getT1(), tuple.getT2())));

//		serieFlux.subscribe(serie -> log.info(serie.toString()));

		StepVerifier
				.create(serieFlux)
				.expectSubscription()
				.expectNext(new Serie("La Casa de Papel", 40), new Serie("Chicago PD", 300))
				.verifyComplete();
	}

	@Test
	public void zipWithOperator() {
		Flux<String> titleFlux = Flux.just("La Casa de Papel", "Chicago PD");
		Flux<Integer> episodesFlux = Flux.just(40, 300);

		Flux<Serie> serieFlux = titleFlux.zipWith(episodesFlux)
				.flatMap(tuple -> Flux.just(new Serie(tuple.getT1(), tuple.getT2())));

		StepVerifier
				.create(serieFlux)
				.expectSubscription()
				.expectNext(new Serie("La Casa de Papel", 40), new Serie("Chicago PD", 300))
				.verifyComplete();
	}

	@AllArgsConstructor
	@Getter
	@ToString
	@EqualsAndHashCode
	class Serie {
		private String title;
		private int episodes;
	}

}
