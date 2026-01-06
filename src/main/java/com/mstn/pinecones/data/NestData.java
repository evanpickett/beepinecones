package com.mstn.pinecones.data;

/**
 * Simple data holder for nest count and bees inside nests.
 * Used by BeeMixin for love mode population calculations.
 */
public record NestData(int nestCount, int beesInNests) {}
