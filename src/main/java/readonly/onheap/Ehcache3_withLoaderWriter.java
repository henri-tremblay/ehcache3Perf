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
package readonly.onheap;

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
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.spi.loaderwriter.CacheLoaderWriter;

import java.io.File;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static io.rainfall.configuration.ReportingConfig.html;
import static io.rainfall.configuration.ReportingConfig.report;
import static io.rainfall.execution.Executions.during;
import static io.rainfall.execution.Executions.times;
import static org.ehcache.config.builders.ResourcePoolsBuilder.heap;
import static utils.Ehcache3Stats.findOperationStat;

/**
 * analyze churn with $JAVA_HOME/bin/jmc
 * @author Ludovic Orban
 */
public class Ehcache3_withLoaderWriter {

  public static void main(String[] args) throws Exception {
    final int nbElementsPerThread = 100000;
    CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
//        .using(new DefaultSerializationProviderConfiguration()
//            .addSerializerFor(Long.class, (Class) CompactJavaSerializer.class)
//            .addSerializerFor(String.class, (Class) CompactJavaSerializer.class)
//        )
        .withCache("cache1", CacheConfigurationBuilder.newCacheConfigurationBuilder(Long.class, String.class, heap(nbElementsPerThread))
//            .withKeySerializingCopier().withValueSerializingCopier()
            .withLoaderWriter(new CacheLoaderWriter<Long, String>() {
              @Override
              public String load(Long key) throws Exception {
                return null;
              }

              @Override
              public Map<Long, String> loadAll(Iterable<? extends Long> keys) throws Exception {
                return null;
              }

              @Override
              public void write(Long key, String value) throws Exception {

              }

              @Override
              public void writeAll(Iterable<? extends Map.Entry<? extends Long, ? extends String>> entries) throws Exception {

              }

              @Override
              public void delete(Long key) throws Exception {

              }

              @Override
              public void deleteAll(Iterable<? extends Long> keys) throws Exception {

              }
            })
            .build())
        .build(true);

    final Cache<Long, String> cache1 = cacheManager.getCache("cache1", Long.class, String.class);

    LongGenerator keyGenerator = new LongGenerator();
    StringGenerator valueGenerator = new StringGenerator(4096);

    CacheConfig<Long, String> cacheConfig = new CacheConfig<Long, String>();
    cacheConfig.cache("cache1", cache1);

    final File reportPath = new File("target/rainfall/" + Ehcache3_withLoaderWriter.class.getName().replace('.', '/'));
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

    Timer t = new Timer(true);
    t.schedule(new TimerTask() {
      @Override
      public void run() {
        long computes = findOperationStat(cache1, "compute", "onheap-store").sum();
        System.out.println("             computes: " + computes);
      }
    }, 1000, 1000);


    System.out.println("testing...");

    Runner.setUp(
        Scenario.scenario("Testing phase")
            .exec(
                Ehcache3Operations.get(Long.class, String.class).using(keyGenerator, valueGenerator)
                    .atRandom(Distribution.GAUSSIAN, 0, nbElementsPerThread, nbElementsPerThread/10)
            ))
        .executed(during(120, TimeDivision.seconds))
        .config(
            ConcurrencyConfig.concurrencyConfig().threads(Runtime.getRuntime().availableProcessors()),
            report(EhcacheResult.class, new EhcacheResult[] {EhcacheResult.GET, EhcacheResult.MISS}).log(html(reportPath.getPath())),
            cacheConfig)
        .start();

    cacheManager.close();

    System.exit(0);
  }

}
