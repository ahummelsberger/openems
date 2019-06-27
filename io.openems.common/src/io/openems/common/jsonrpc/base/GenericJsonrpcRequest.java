package io.openems.common.jsonrpc.base;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a generic JSON-RPC Request.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": string,
 *   "params": {}
 * }
 * </pre>
 * 
 * @see <a href="https://www.jsonrpc.org/specification#request_object">JSON-RPC
 *      specification</a>
 */
public class GenericJsonrpcRequest extends JsonrpcRequest {

	public static GenericJsonrpcRequest from(String json) throws OpenemsNamedException {
		return from(JsonUtils.parseToJsonObject(json));
	}

	public static GenericJsonrpcRequest from(JsonObject j) throws OpenemsNamedException {
		String id = JsonUtils.getAsString(j, "id");
		String method = JsonUtils.getAsString(j, "method");
		JsonObject params = JsonUtils.getAsJsonObject(j, "params");
		return new GenericJsonrpcRequest(UUID.fromString(id), method, params);
	}

	private final JsonObject params;

	public GenericJsonrpcRequest(UUID id, String method, JsonObject params) {
		super(id, method);
		this.params = params;
	}

	@Override
	public JsonObject getParams() {
		return this.params;
	}

}
