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

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.terracotta.statistics.OperationStatistic;
import org.terracotta.statistics.StatisticsManager;
import org.terracotta.statistics.ValueStatistic;

import static org.ehcache.config.builders.ResourcePoolsBuilder.heap;
import static utils.Ehcache3Stats.findOperationStat;
import static utils.Ehcache3Stats.findValueStat;

/**
 * @author Ludovic Orban
 */
public class StatMain {

  public static void main(String[] args) throws Exception {
    System.out.println(StatisticsManager.class);


    CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
        .withCache("cache1", CacheConfigurationBuilder.newCacheConfigurationBuilder(Long.class, String.class, heap(1000L).offheap(10, MemoryUnit.MB))
            .build())
        .build(true);

    final Cache<Long, String> cache1 = cacheManager.getCache("cache1", Long.class, String.class);


    for (long i=0L;i<1000000L;i++) {
      cache1.put(i, "abcd");
      cache1.get(i);
    }

    System.out.println(findValueStat(cache1, "size", "onheap-store").value());

    System.out.println(findValueStat(cache1, "allocatedMemory", "local-offheap").value());
    System.out.println(findValueStat(cache1, "occupiedMemory", "local-offheap").value());
    System.out.println(findValueStat(cache1, "totalSize", "local-offheap").value());
    System.out.println(findValueStat(cache1, "tableCapacity", "local-offheap").value());
    System.out.println(findValueStat(cache1, "usedSlotCount", "local-offheap").value());
    System.out.println(findValueStat(cache1, "removedSlotCount", "local-offheap").value());
    System.out.println(findValueStat(cache1, "reprobeLength", "local-offheap").value());


    cacheManager.close();

  }

}
