package org.skriptlang.skript.docs.transformers;

import com.google.gson.Gson;
import org.jetbrains.annotations.ApiStatus;
import org.skriptlang.skript.docs.generators.GenerationResult;

import java.util.List;

@ApiStatus.Internal
public final class JsonTransformer implements Transformer<String> {
	
	private static final Gson GSON = new Gson();
	
	@Override
	public String transform(GenerationResult result) {
		return GSON.toJson(result);
	}
	
	@Override
	public String combine(String a, String b) {
		return a + "," + b;
	}
	
	@Override
	public String transform(List<GenerationResult> results) {
		return "{\"elements\":[" + Transformer.super.transform(results) + "]}";
	}
	
}
