# Performance Optimization

## Caching and Memoization

- **Apply caching strategies** to reduce redundant calculations and optimize performance.

### Use Standard Kotlin Collections

  ```kotlin:path/to/standard_kotlin_collections_example.kt
  private val strategicCache = mutableMapOf<CacheKey, Float>()

  /*
   * Retrieves the strategic value of a tile, using caching to avoid recalculations.
   *
   * @param tile The tile to evaluate.
   * @param civ The civilization evaluating the tile.
   * @return The strategic value of the tile.
   */
  fun getStrategicValue(tile: Tile, civ: Civilization): Float {
      return strategicCache.getOrPut(CacheKey(tile, civ)) {
          calculateStrategicValue(tile, civ)
      }
  }
  ```

### Lazy Property Delegation

  ```kotlin:path/to/lazy_property_delegation_example.kt
  /*
   * 1. Lazily computes a list of capital city center tiles.
   */
  private val capitalCityCenters: List<Tile> by lazy {
      // 2. Filter cities that are capitals.
      // 3. Map to their center tiles.
      cities.filter { it.isCapital() }.map { it.getCenterTile() }
  }
  ```

### Composite Cache Keys

  ```kotlin:path/to/composite_cache_keys_example.kt
  /*
   * 1. Data class representing a composite cache key.
   */
  private data class CacheKey(val tile: Tile, val civ: Civilization)
  private val strategicCache = mutableMapOf<CacheKey, Float>()

  /*
   * 2. Retrieves the strategic value of a tile, using caching to avoid recalculations.
   *
   * @param tile The tile to evaluate.
   * @param civ The civilization evaluating the tile.
   * @return The strategic value of the tile.
   */
  fun getStrategicValue(tile: Tile, civ: Civilization): Float {
      return strategicCache.getOrPut(CacheKey(tile, civ)) {
          calculateStrategicValue(tile, civ)
      }
  }
  ```

### Coroutine-Based Async Caching

  ```kotlin:path/to/coroutine_based_async_caching_example.kt
  private val pathCache = mutableMapOf<Pair<Tile, Tile>, List<Tile>>()

  /*
   * Retrieves the path between two tiles, caching the result for future use.
   *
   * @param start The starting tile.
   * @param end The ending tile.
   * @return The path between the tiles.
   */
  suspend fun getPathBetweenTiles(start: Tile, end: Tile): List<Tile> = coroutineScope {
      pathCache.getOrPut(Pair(start, end)) {
          calculatePath(start, end)
      }
  }
  ```

### Cache Size Management

  ```kotlin:path/to/cache_size_management_example.kt
  /*
   * 1. A bounded cache that limits its size to a maximum number of entries.
   */
  class BoundedCache<K, V>(private val maxSize: Int) {
      private val cache = LinkedHashMap<K, V>()

      /*
       * 2. Puts a value into the cache, ensuring the cache does not exceed maxSize.
       *
       * @param key The key to store the value under.
       * @param value The value to store.
       */
      fun put(key: K, value: V) {
          if (cache.size >= maxSize) {
              // 3. Remove the eldest entry to make space.
              cache.remove(cache.keys.first())
          }
          cache[key] = value
      }
  }
  ```

### Local Cache for Expensive Calculations

  ```kotlin:path/to/local_cache_example.kt
  /*
   * 1. Evaluates tiles using a local cache to store intermediate results.
   */
  class TileEvaluator {
      /*
       * 2. Evaluates a list of tiles for strategic value.
       *
       * @param tiles The list of tiles to evaluate.
       * @param civ The civilization performing the evaluation.
       */
      fun evaluateTiles(tiles: List<Tile>, civ: Civilization) {
          val localCache = mutableMapOf<Tile, TileStats>()  // 3. Temporary calculation cache
          tiles.forEach { tile ->
              // 4. Retrieve or compute the tile stats.
              val stats = localCache.getOrPut(tile) {
                  tile.stats.getTileStats(civ)
              }
              // 5. Use stats in evaluation.
          }
      }
  }
  ```

### Turn-Based Cache Invalidation

  ```kotlin:path/to/turn_based_cache_invalidation_example.kt
  /*
   * 1. A cache that invalidates its entries when the game turn advances.
   */
  class TurnCache<T>(private val gameInfo: GameInfo) {
      private var cachedTurn = -1
      private var cachedValue: T? = null

      /*
       * 2. Retrieves a cached value, recalculating it if the game turn has advanced.
       *
       * @param calculator The function to calculate the value.
       * @return The cached or newly calculated value.
       */
      fun getValue(calculator: () -> T): T {
          if (cachedTurn != gameInfo.turns) {
              // 3. Recalculate and update the cache.
              cachedValue = calculator()
              cachedTurn = gameInfo.turns
          }
          return cachedValue!!
      }
  }
  ```

## Parallel Processing

- **Utilize Kotlin coroutines** for parallel execution of AI tasks to enhance performance.

  ```kotlin:path/to/parallel_processing_example.kt
  /*
   * Makes AI decisions for economy and military in parallel, scaled by personality traits.
   *
   * @param gameState The current state of the game.
   * @param personality The personality traits of the civ.
   * @return A combined list of decisions from both modules.
   */
  suspend fun makeDecisions(gameState: GameState, personality: Personality): List<Decision> = coroutineScope {
      // 1. Launch asynchronous tasks for each AI module.
      val economyDecision = async { economyManager.makeDecision(gameState, personality) }
      val militaryDecision = async { militaryManager.makeDecision(gameState, personality) }
      // 2. Await and combine decisions.
      listOf(economyDecision.await(), militaryDecision.await()).flatten()
  }
  ```

## Efficient Data Structures

- **Use standard Kotlin collections** for managing game data.

  ```kotlin:path/to/efficient_data_structures_example.kt
  /*
   * 1. Manages the units and production items using standard Kotlin collections.
   */
  val units: MutableList<Unit> = mutableListOf()  // 2. Mutable list for managing units
  val cityProduction: MutableMap<Int, ProductionItem> = mutableMapOf()  // 3. Mutable map for city production items
  ```