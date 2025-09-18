package com.recommendation.core.hashtable;

import java.util.*;

/**
 * Custom Hash Table implementation for fast user/product lookups
 * Core DSA: Hash Table with collision resolution using chaining
 */
public class CustomHashMap<K, V> {
    private static final int DEFAULT_CAPACITY = 16;
    private static final double LOAD_FACTOR_THRESHOLD = 0.75;

    private Node<K, V>[] buckets;
    private int size;
    private int capacity;

    @SuppressWarnings("unchecked")
    public CustomHashMap() {
        this.capacity = DEFAULT_CAPACITY;
        this.buckets = new Node[capacity];
        this.size = 0;
    }

    @SuppressWarnings("unchecked")
    public CustomHashMap(int initialCapacity) {
        this.capacity = Math.max(initialCapacity, DEFAULT_CAPACITY);
        this.buckets = new Node[capacity];
        this.size = 0;
    }

    /**
     * Node class for chaining collision resolution
     */
    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
            this.next = null;
        }
    }

    /**
     * Hash function using multiplication method
     */
    private int hash(K key) {
        if (key == null) return 0;

        int hash = key.hashCode();
        // Apply secondary hash to reduce collisions
        hash ^= (hash >>> 16);
        return Math.abs(hash % capacity);
    }

    /**
     * Put key-value pair into hash table
     * Time Complexity: O(1) average, O(n) worst case
     */
    public V put(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        // Check if resize is needed
        if ((double) size / capacity >= LOAD_FACTOR_THRESHOLD) {
            resize();
        }

        int index = hash(key);
        Node<K, V> head = buckets[index];

        // Check if key already exists
        Node<K, V> current = head;
        while (current != null) {
            if (current.key.equals(key)) {
                V oldValue = current.value;
                current.value = value;
                return oldValue;
            }
            current = current.next;
        }

        // Add new node at the beginning of chain
        Node<K, V> newNode = new Node<>(key, value);
        newNode.next = head;
        buckets[index] = newNode;
        size++;

        return null;
    }

    /**
     * Get value by key
     * Time Complexity: O(1) average, O(n) worst case
     */
    public V get(K key) {
        if (key == null) return null;

        int index = hash(key);
        Node<K, V> current = buckets[index];

        while (current != null) {
            if (current.key.equals(key)) {
                return current.value;
            }
            current = current.next;
        }

        return null;
    }

    /**
     * Remove key-value pair
     * Time Complexity: O(1) average, O(n) worst case
     */
    public V remove(K key) {
        if (key == null) return null;

        int index = hash(key);
        Node<K, V> current = buckets[index];
        Node<K, V> prev = null;

        while (current != null) {
            if (current.key.equals(key)) {
                if (prev == null) {
                    buckets[index] = current.next;
                } else {
                    prev.next = current.next;
                }
                size--;
                return current.value;
            }
            prev = current;
            current = current.next;
        }

        return null;
    }

    /**
     * Check if key exists
     */
    public boolean containsKey(K key) {
        return get(key) != null;
    }

    /**
     * Check if value exists
     */
    public boolean containsValue(V value) {
        for (Node<K, V> head : buckets) {
            Node<K, V> current = head;
            while (current != null) {
                if (Objects.equals(current.value, value)) {
                    return true;
                }
                current = current.next;
            }
        }
        return false;
    }

    /**
     * Get all keys
     */
    public Set<K> keySet() {
        Set<K> keys = new HashSet<>();
        for (Node<K, V> head : buckets) {
            Node<K, V> current = head;
            while (current != null) {
                keys.add(current.key);
                current = current.next;
            }
        }
        return keys;
    }

    /**
     * Get all values
     */
    public Collection<V> values() {
        List<V> values = new ArrayList<>();
        for (Node<K, V> head : buckets) {
            Node<K, V> current = head;
            while (current != null) {
                values.add(current.value);
                current = current.next;
            }
        }
        return values;
    }

    /**
     * Get all key-value pairs
     */
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> entries = new HashSet<>();
        for (Node<K, V> head : buckets) {
            Node<K, V> current = head;
            while (current != null) {
                entries.add(new AbstractMap.SimpleEntry<>(current.key, current.value));
                current = current.next;
            }
        }
        return entries;
    }

    /**
     * Resize hash table when load factor exceeds threshold
     */
    @SuppressWarnings("unchecked")
    private void resize() {
        Node<K, V>[] oldBuckets = buckets;
        int oldCapacity = capacity;

        capacity *= 2;
        buckets = new Node[capacity];
        size = 0;

        // Rehash all elements
        for (int i = 0; i < oldCapacity; i++) {
            Node<K, V> current = oldBuckets[i];
            while (current != null) {
                put(current.key, current.value);
                current = current.next;
            }
        }
    }

    /**
     * Get size of hash table
     */
    public int size() {
        return size;
    }

    /**
     * Check if hash table is empty
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Clear all entries
     */
    public void clear() {
        Arrays.fill(buckets, null);
        size = 0;
    }

    /**
     * Get current load factor
     */
    public double getLoadFactor() {
        return (double) size / capacity;
    }

    /**
     * Get number of non-empty buckets
     */
    public int getUsedBuckets() {
        int used = 0;
        for (Node<K, V> head : buckets) {
            if (head != null) used++;
        }
        return used;
    }

    /**
     * Get maximum chain length for analysis
     */
    public int getMaxChainLength() {
        int maxLength = 0;
        for (Node<K, V> head : buckets) {
            int length = 0;
            Node<K, V> current = head;
            while (current != null) {
                length++;
                current = current.next;
            }
            maxLength = Math.max(maxLength, length);
        }
        return maxLength;
    }

    /**
     * Get hash table statistics
     */
    public String getStatistics() {
        return "CustomHashMap Statistics: " +
                "Size=" + size +
                ", Capacity=" + capacity +
                ", LoadFactor=" + String.format("%.2f", getLoadFactor()) +
                ", UsedBuckets=" + getUsedBuckets() +
                ", MaxChainLength=" + getMaxChainLength();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;

        for (Node<K, V> head : buckets) {
            Node<K, V> current = head;
            while (current != null) {
                if (!first) sb.append(", ");
                sb.append(current.key).append("=").append(current.value);
                first = false;
                current = current.next;
            }
        }

        sb.append("}");
        return sb.toString();
    }
}
