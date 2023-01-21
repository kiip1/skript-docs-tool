package org.skriptlang.skript.docs;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.skriptlang.skript.Skript;

@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface Documentation {
	
	/**
	 * @return {@link Documentation}
	 */
	@Contract("_ -> new")
	static Documentation of(Skript skript) {
		return new DocumentationImpl(skript);
	}
	
	/**
	 * @return {@link Skript}
	 */
	Skript skript();
	
}
