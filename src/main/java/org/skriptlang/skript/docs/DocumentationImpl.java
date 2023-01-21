package org.skriptlang.skript.docs;

import org.skriptlang.skript.Skript;

final class DocumentationImpl implements Documentation {
	
	private final Skript skript;
	
	DocumentationImpl(Skript skript) {
		this.skript = skript;
	}
	
	@Override
	public Skript skript() {
		return skript;
	}
	
}
