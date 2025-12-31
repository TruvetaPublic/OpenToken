# Performance Tuning

Guidance to optimize OpenToken throughput and resource usage.

---

## Overview

OpenToken performance depends on:

- Input file format (CSV vs Parquet)
- Processing mode (encryption vs hash-only)
- Runtime environment (local, Docker, Spark)
- Memory allocation
- Parallelization strategy

---

## CSV vs Parquet Performance

| Aspect           | CSV                        | Parquet                   |
| ---------------- | -------------------------- | ------------------------- |
| **Read speed**   | Slower (text parsing)      | Faster (binary, columnar) |
| **Write speed**  | Moderate                   | Faster (with compression) |
| **File size**    | Larger                     | Smaller (compressed)      |
| **Memory usage** | Higher (string processing) | Lower (columnar access)   |
| **Best for**     | Small files, debugging     | Large files, production   |

**Recommendation:** Use Parquet for datasets >10,000 records.

```bash
# Convert input to Parquet
java -jar opentoken-cli-*.jar \
  -i data.parquet -t parquet \
  -o tokens.parquet \
  -h "HashingKey" -e "EncryptionKey"
```

---

## Encryption vs Hash-Only Mode

| Mode           | Speed          | Token Size    | Use Case          |
| -------------- | -------------- | ------------- | ----------------- |
| **Encryption** | Baseline       | ~80-100 chars | External sharing  |
| **Hash-only**  | ~20-30% faster | ~44-64 chars  | Internal matching |

For internal-only matching, hash-only mode is faster:

```bash
java -jar opentoken-cli-*.jar --hash-only \
  -i data.csv -t csv -o tokens.csv -h "HashingKey"
```

---

## Batch Processing

### File Splitting

For large datasets, split files and process in parallel:

```bash
# Split large CSV into 100MB chunks
split -b 100m large_data.csv chunk_

# Process chunks in parallel
for file in chunk_*; do
  java -jar opentoken-cli-*.jar \
    -i "$file" -t csv -o "tokens_$file" \
    -h "HashingKey" -e "EncryptionKey" &
done
wait

# Combine outputs
cat tokens_chunk_* > all_tokens.csv
```

### Parallel Processing Limit

Limit parallel jobs to available CPU cores:

```bash
# Using GNU parallel (if available)
ls data_*.csv | parallel -j $(nproc) \
  'java -jar opentoken-cli-*.jar -i {} -t csv -o tokens_{} -h "Key" -e "Key32"'
```

---

## JVM Tuning (Java)

### Memory Allocation

For large files (1GB+), increase heap size:

```bash
java -Xmx4g -jar opentoken-cli-*.jar \
  -i large_data.csv -t csv -o tokens.csv \
  -h "HashingKey" -e "EncryptionKey"
```

| File Size     | Recommended Heap         |
| ------------- | ------------------------ |
| < 100 MB      | Default (256MB-1GB)      |
| 100 MB - 1 GB | `-Xmx2g`                 |
| 1 GB - 5 GB   | `-Xmx4g`                 |
| > 5 GB        | Use Spark or split files |

### Garbage Collection

For sustained processing, use G1GC:

```bash
java -Xmx4g -XX:+UseG1GC -jar opentoken-cli-*.jar ...
```

---

## Python Runtime Tuning

### Streaming I/O

Python CLI uses streaming I/O to minimize memory footprint. No special tuning is typically needed.

### Virtual Environment

Ensure you're using the project venv for optimal dependencies:

```bash
source /workspaces/OpenToken/.venv/bin/activate
python -m opentoken_cli.main ...
```

---

## Spark Tuning

### Partitioning

Adjust shuffle partitions based on cluster size:

```python
# For small clusters (< 10 nodes)
spark.conf.set("spark.sql.shuffle.partitions", "100")

# For large clusters (> 10 nodes)
spark.conf.set("spark.sql.shuffle.partitions", "500")
```

### Executor Memory

```python
spark = SparkSession.builder \
    .config("spark.executor.memory", "4g") \
    .config("spark.driver.memory", "2g") \
    .getOrCreate()
```

### Coalesce Output

Reduce output file count:

```python
tokens_df.coalesce(10).write.mode("overwrite").parquet("output/")
```

### Broadcast Variables

For overlap analysis with small reference datasets:

```python
from pyspark.sql.functions import broadcast

result = large_df.join(broadcast(small_df), "Token")
```

---

## Docker Tuning

### Memory Limits

Set container memory limits for predictable performance:

```bash
docker run --rm -m 4g \
  -v $(pwd)/resources:/app/resources \
  opentoken:latest ...
```

### CPU Limits

```bash
docker run --rm --cpus="4" \
  -v $(pwd)/resources:/app/resources \
  opentoken:latest ...
```

---

## Benchmarking

### Measure Processing Time

```bash
time java -jar opentoken-cli-*.jar \
  -i data.csv -t csv -o tokens.csv \
  -h "HashingKey" -e "EncryptionKey"
```

### Check Metadata for Insights

Metadata includes processing statistics:

```bash
cat tokens.metadata.json | jq '{
  TotalRows,
  TotalRowsWithInvalidAttributes,
  ProcessingTimestamp
}'
```

**TODO:** Add `ProcessingDurationMs` field to metadata for benchmarking.

---

## Performance Baselines

**TODO:** Document expected throughput baselines:

| Environment      | Records/Second | Notes                  |
| ---------------- | -------------- | ---------------------- |
| Local (Java)     | TBD            | Single-threaded CLI    |
| Local (Python)   | TBD            | Streaming I/O          |
| Docker           | TBD            | Container overhead     |
| Spark (10 nodes) | TBD            | Distributed processing |

---

## Troubleshooting Performance

| Problem              | Solution                               |
| -------------------- | -------------------------------------- |
| Out of memory (Java) | Increase heap: `-Xmx4g`                |
| Slow CSV parsing     | Switch to Parquet format               |
| High CPU usage       | Limit parallel jobs to CPU core count  |
| Spark shuffle spill  | Increase executor memory or partitions |
| Docker slow          | Ensure volume mounts use native paths  |

---

## Next Steps

- **Batch processing**: [Running Batch Jobs](../operations/running-batch-jobs.md)
- **Spark processing**: [Spark or Databricks](../operations/spark-or-databricks.md)
- **Configuration options**: [Configuration](configuration.md)
