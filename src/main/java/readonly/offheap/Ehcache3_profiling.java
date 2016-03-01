/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package readonly.offheap;

import io.rainfall.Runner;
import io.rainfall.Scenario;
import io.rainfall.configuration.ConcurrencyConfig;
import io.rainfall.ehcache.statistics.EhcacheResult;
import io.rainfall.ehcache3.CacheConfig;
import io.rainfall.ehcache3.Ehcache3Operations;
import io.rainfall.generator.LongGenerator;
import io.rainfall.generator.StringGenerator;
import io.rainfall.generator.sequence.Distribution;
import io.rainfall.unit.TimeDivision;
import net.sf.ehcache.util.concurrent.LongAdder;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.statistics.AuthoritativeTierOperationOutcomes;
import org.ehcache.core.statistics.CachingTierOperationOutcomes;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expiry;
import readonly.utils.ProfilingCompactJavaSerializer;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import static io.rainfall.configuration.ReportingConfig.html;
import static io.rainfall.configuration.ReportingConfig.report;
import static io.rainfall.execution.Executions.during;
import static io.rainfall.execution.Executions.times;
import static readonly.utils.Ehcache3Stats.findStat;

/**
 * @author Ludovic Orban
 */
public class Ehcache3_profiling {

  public static void main(String[] args) throws Exception {
    final LongAdder expiryCounter = new LongAdder();
    final ProfilingCompactJavaSerializer keySerializer = new ProfilingCompactJavaSerializer(ClassLoader.getSystemClassLoader());
    final ProfilingCompactJavaSerializer valueSerializer = new ProfilingCompactJavaSerializer(ClassLoader.getSystemClassLoader());
    CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()

        .withCache("cache1", CacheConfigurationBuilder.newCacheConfigurationBuilder(Long.class, String.class)
            .withKeySerializer(keySerializer)
            .withValueSerializer(valueSerializer)
            .withExpiry(new Expiry<Long, String>() {
              @Override
              public Duration getExpiryForCreation(Long key, String value) {
                return Duration.FOREVER;
              }

              @Override
              public Duration getExpiryForAccess(Long key, String value) {
                expiryCounter.increment();
                return Duration.FOREVER;
              }

              @Override
              public Duration getExpiryForUpdate(Long key, String oldValue, String newValue) {
                return Duration.FOREVER;
              }
            })
            .withResourcePools(ResourcePoolsBuilder.newResourcePoolsBuilder()
                .heap(1000, EntryUnit.ENTRIES).offheap(2, MemoryUnit.GB))
            .build())
        .build(true);

    final Cache<Long, String> cache1 = cacheManager.getCache("cache1", Long.class, String.class);

    LongGenerator keyGenerator = new LongGenerator();
    StringGenerator valueGenerator = new StringGenerator(4096);

    CacheConfig<Long, String> cacheConfig = new CacheConfig<Long, String>();
    cacheConfig.cache("cache1", cache1);

    final int nbElementsPerThread = 100000;
    final File reportPath = new File("target/rainfall/offheap/ehcache3");
    Runner.setUp(
        Scenario.scenario("Loading phase")
            .exec(
                Ehcache3Operations.put(Long.class, String.class).using(keyGenerator, valueGenerator)
                    .sequentially()
            ))
        .executed(times(nbElementsPerThread))
        .config(
            ConcurrencyConfig.concurrencyConfig().threads(1),
            report(EhcacheResult.class),
            cacheConfig)
        .start();

    System.out.println("testing...");

    Timer t = new Timer(true);
    t.schedule(new TimerTask() {
      @Override
      public void run() {
        long onHeapHits = findStat(cache1, "getOrComputeIfAbsent", "onheap-store").count(CachingTierOperationOutcomes.GetOrComputeIfAbsentOutcome.HIT);
        long offHeapHits = findStat(cache1, "computeIfAbsentAndFault", "local-offheap").count(AuthoritativeTierOperationOutcomes.ComputeIfAbsentAndFaultOutcome.HIT);
        long total = onHeapHits + offHeapHits;
        System.out.println();
        System.out.println("        heap hits: " + onHeapHits);
        System.out.println("     offheap hits: " + offHeapHits);
        System.out.printf ("   heap hit ratio: %.1f%%\n", ((double) onHeapHits / total * 100.0));
        System.out.printf ("offheap hit ratio: %.1f%%\n", ((double) offHeapHits / total * 100.0));
        System.out.println("         expiries: " + expiryCounter.sum());
      }
    }, 1000, 1000);


    Runner.setUp(
        Scenario.scenario("Testing phase")
            .exec(
                Ehcache3Operations.get(Long.class, String.class).using(keyGenerator, valueGenerator)
                    .atRandom(Distribution.GAUSSIAN, 0, nbElementsPerThread, nbElementsPerThread/10)
            ))
        .executed(during(10, TimeDivision.seconds))
        .config(
            ConcurrencyConfig.concurrencyConfig().threads(Runtime.getRuntime().availableProcessors()),
            report(EhcacheResult.class, new EhcacheResult[] {EhcacheResult.GET, EhcacheResult.MISS}).log(html(reportPath.getPath())),
            cacheConfig)
        .start();

    cacheManager.close();

    System.out.println("*** key equals true ***");
    System.out.println(keySerializer.equalsTrue);
    System.out.println("*** key equals false ***");
    System.out.println(keySerializer.equalsFalse);
    System.out.println("*** key reads ***");
    System.out.println(keySerializer.reads);
    System.out.println("*** key equals ***");
    System.out.println(keySerializer.equals);
    System.out.println("*** value reads ***");
    System.out.println(valueSerializer.reads);
    System.out.println("*** value equals ***");
    System.out.println(valueSerializer.equals);
    System.out.println("*** *** *** ***");


    System.exit(0);
  }

}
