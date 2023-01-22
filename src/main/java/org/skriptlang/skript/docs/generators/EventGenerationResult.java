package org.skriptlang.skript.docs.generators;

import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;

import java.util.List;

final class EventGenerationResult implements GenerationResult {
	
	private final BukkitSyntaxInfos.Event<?> info;
	private final Class<?> clazz;
	
	public EventGenerationResult(BukkitSyntaxInfos.Event<?> info, Class<?> clazz) {
		this.info = info;
		this.clazz = clazz;
	}
	
	@Override
	public String documentationId() {
		String documentationId = info.documentationId();
		return documentationId == null ? info.id() : documentationId;
	}
	
	@Override
	public String name() {
		return info.name();
	}
	
	@Override
	public String[] description() {
		return info.description().toArray(new String[0]);
	}
	
	@Override
	public String[] examples() {
		return info.examples().toArray(new String[0]);
	}
	
	@Override
	public String since() {
		return info.since();
	}
	
	@Override
	public Class<?> type() {
		return clazz;
	}
	
	@Override
	public List<String> patterns() {
		return info.patterns();
	}
	
	@Override
	public SyntaxInfo<?> info() {
		return info;
	}
	
	@Override
	@Nullable
	public String[] requiredPlugins() {
		return info.requiredPlugins().toArray(new String[0]);
	}
	
	@Override
	public String[] keywords() {
		return info.keywords().toArray(new String[0]);
	}
	
	@Override
	public String[] events() {
		return new String[0];
	}
	
	@Override
	public boolean deprecated() {
		return false;
	}
	
	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other == null || getClass() != other.getClass())
			return false;
		EventGenerationResult result = (EventGenerationResult) other;
		return info.equals(result.info);
	}
	
	@Override
	public int hashCode() {
		return info.hashCode();
	}
	
}
