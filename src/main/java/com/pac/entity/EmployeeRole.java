package com.pac.entity;

public enum EmployeeRole {
	ADMIN, GUARD, REGULAR;

	@Override
	public String toString() { return this.name(); }
}
