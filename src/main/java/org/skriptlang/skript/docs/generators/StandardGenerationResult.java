package org.skriptlang.skript.docs.generators;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.DocumentationId;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.util.MarkedForRemoval;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

final class StandardGenerationResult implements GenerationResult {
	
	private final SyntaxInfo<?> info;
	private final Class<?> clazz;
	
	public StandardGenerationResult(SyntaxInfo<?> info, Class<?> clazz) {
		this.info = info;
		this.clazz = clazz;
	}
	
	@Override
	public String documentationId() {
		return consumer(DocumentationId.class, DocumentationId::value, clazz::getSimpleName);
	}
	
	@Override
	public String name() {
		return fail(Name.class, Name::value);
	}
	
	@Override
	public String[] description() {
		return fail(Description.class, Description::value);
	}
	
	@Override
	public String[] examples() {
		return fail(Examples.class, Examples::value);
	}
	
	@Override
	public String since() {
		return fail(Since.class, Since::value);
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
		return consumer(RequiredPlugins.class, RequiredPlugins::value, () -> new String[0]);
	}
	
	@Override
	public String[] keywords() {
		return consumer(Keywords.class, Keywords::value, () -> new String[0]);
	}
	
	@Override
	public String[] events() {
		return consumer(Events.class, Events::value, () -> new String[0]);
	}
	
	@Override
	public boolean deprecated() {
		return consumer(MarkedForRemoval.class, annotation -> true, () -> false);
	}
	
	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other == null || getClass() != other.getClass())
			return false;
		StandardGenerationResult result = (StandardGenerationResult) other;
		return info.equals(result.info);
	}
	
	@Override
	public int hashCode() {
		return info.hashCode();
	}
	
	private <T extends Annotation, R> R consumer(Class<? extends T> clazz, Function<T, R> consumer, Supplier<R> fallback) {
		T annotation = this.clazz.getDeclaredAnnotation(clazz);
		if (annotation == null) return fallback.get();
		R result = consumer.apply(annotation);
		return result == null ? fallback.get() : result;
	}
	
	private <T extends Annotation, R> R fail(Class<? extends T> clazz, Function<T, R> consumer) {
		T annotation = this.clazz.getDeclaredAnnotation(clazz);
		if (annotation == null) throw new IllegalStateException("Annotation must be present");
		R result = consumer.apply(annotation);
		if (result == null) throw new IllegalStateException("Value may not be null");
		return result;
	}
	
}
