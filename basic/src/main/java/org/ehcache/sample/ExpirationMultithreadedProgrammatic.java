package org.ehcache.sample;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.slf4j.Logger;

import java.time.Duration;

import static org.ehcache.config.builders.CacheConfigurationBuilder.newCacheConfigurationBuilder;
import static org.ehcache.config.builders.CacheManagerBuilder.newCacheManagerBuilder;
import static org.ehcache.config.builders.ResourcePoolsBuilder.heap;
import static org.ehcache.config.units.MemoryUnit.MB;
import static org.slf4j.LoggerFactory.getLogger;

public class ExpirationMultithreadedProgrammatic {
  private static final Logger LOGGER = getLogger(ExpirationMultithreadedProgrammatic.class);

  public static void main(String[] args) {
    LOGGER.info("Creating cache manager programmatically");
    String cacheAlias = "basicCache";
    CacheConfigurationBuilder<String, String> configurationBuilder =
            newCacheConfigurationBuilder(String.class, String.class, heap(100).offheap(1, MB))
                    .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(2)));
    try {
      CacheManager cacheManager = newCacheManagerBuilder()
              .withCache(cacheAlias, configurationBuilder)
              .build(true);
      Cache<String, String> expiryCache = cacheManager.getCache(cacheAlias, String.class, String.class);

      exerciseCache(expiryCache);
      Thread.sleep(3000);

      LOGGER.info("Closing cache manager");
      cacheManager.close();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    LOGGER.info("Exiting");
  }

  private static void exerciseCache(Cache<String, String> expiryCache) {
    LOGGER.info("Spawning threads");
    Runnable r = () -> {
      String key = "wow, a random key!";
      LOGGER.info("Current cached value: '{}'", expiryCache.get(key));
      LOGGER.info("Putting to cache");
      expiryCache.put(key, "an amazing value goes here!");
      String value = expiryCache.get(key);
      LOGGER.info("Retrieved '{}'", value);

      LOGGER.info("Let cache expire and try again");
      try {
        Thread.sleep(2001);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      value = expiryCache.get(key);
      LOGGER.info("Retrieved '{}'", value);
    };
    for (int i = 0; i < 10; i++) {
      Thread t = new Thread(r);
      // Let's run Thread in background..
      // Sometimes you need to run thread in background for your Timer application..
      t.start();
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
