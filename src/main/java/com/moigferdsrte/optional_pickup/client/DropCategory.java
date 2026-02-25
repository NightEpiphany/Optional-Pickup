package com.moigferdsrte.optional_pickup.client;

public enum DropCategory {
	TOOLS(0),
	COMBAT(1),
	ARMOR(2),
	BLOCKS(3),
	FOOD(4),
	MISC(5);

	private final int sortKey;

	DropCategory(int sortKey) {
		this.sortKey = sortKey;
	}

	public int sortKey() {
		return this.sortKey;
	}
}
