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
package offheap;

import io.rainfall.Runner;
import io.rainfall.Scenario;
import io.rainfall.configuration.ConcurrencyConfig;
import io.rainfall.ehcache.statistics.EhcacheResult;
import io.rainfall.ehcache2.CacheConfig;
import io.rainfall.ehcache2.Ehcache2Operations;
import io.rainfall.generator.sequence.Distribution;
import io.rainfall.unit.TimeDivision;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.MemoryUnit;
import utils.LongWrapper;
import utils.LongWrapperGenerator;
import utils.StringWrapper;
import utils.StringWrapperGenerator;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import static io.rainfall.configuration.ReportingConfig.html;
import static io.rainfall.configuration.ReportingConfig.report;
import static io.rainfall.execution.Executions.during;
import static io.rainfall.execution.Executions.times;

/**
 * @author Ludovic Orban
 */
public class Ehcache2_serializable {

  public static void main(String[] args) throws Exception {
    System.setProperty("com.tc.productkey.path", System.getProperty("user.home") + "/.tc/terracotta-license.key");
    Configuration configuration = new Configuration();
    CacheConfiguration cacheConfiguration = new CacheConfiguration("cache1", 1000);
    cacheConfiguration.setMaxBytesLocalOffHeap(MemoryUnit.parseSizeInBytes("2G"));
    configuration.addCache(cacheConfiguration);
    CacheManager cacheManager = new CacheManager(configuration);

    final Cache cache1 = cacheManager.getCache("cache1");

    LongWrapperGenerator keyGenerator = new LongWrapperGenerator();
    StringWrapperGenerator valueGenerator = new StringWrapperGenerator(4096);

    CacheConfig<LongWrapper, StringWrapper> cacheConfig = new CacheConfig<LongWrapper, StringWrapper>();
    cacheConfig.caches(cache1);

    final int nbElementsPerThread = 100000;
    final File reportPath = new File("target/rainfall/offheap/ehcache2");
    Runner.setUp(
        Scenario.scenario("Loading phase")
            .exec(
                Ehcache2Operations.put(LongWrapper.class, StringWrapper.class).using(keyGenerator, valueGenerator)
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
        long onHeapHits = cache1.getStatistics().localHeapHitCount();
        long offHeapHits = cache1.getStatistics().localOffHeapHitCount();
        long total = onHeapHits + offHeapHits;
        System.out.println("        heap hits: " + onHeapHits);
        System.out.println("     offheap hits: " + offHeapHits);
        System.out.printf ("   heap hit ratio: %.1f%%\n", ((double) onHeapHits / total * 100.0));
        System.out.printf ("offheap hit ratio: %.1f%%\n", ((double) offHeapHits / total * 100.0));
      }
    }, 1000, 1000);

    Runner.setUp(
        Scenario.scenario("Testing phase")
            .exec(
                Ehcache2Operations.get(LongWrapper.class, StringWrapper.class).using(keyGenerator, valueGenerator)
                    .atRandom(Distribution.GAUSSIAN, 0, nbElementsPerThread, nbElementsPerThread/10)
            ))
        .executed(during(120, TimeDivision.seconds))
        .config(
            ConcurrencyConfig.concurrencyConfig().threads(Runtime.getRuntime().availableProcessors()),
            report(EhcacheResult.class, new EhcacheResult[] {EhcacheResult.GET, EhcacheResult.MISS}).log(html(reportPath.getPath())),
            cacheConfig)
        .start();

     cacheManager.shutdown();

    System.exit(0);
  }

}
