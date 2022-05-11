package nilloader.impl.fixes;

import nilloader.api.lib.mini.MiniTransformer;

public class EarlyMiniTransformer extends MiniTransformer {

	@Override
	protected void $$internal$logDebug(String fmt, Object... params) {}

	@Override
	protected void $$internal$logError(String fmt, Object... params) {
		System.err.printf(fmt.replace("{}", "%s")+"%n", params);
	}
	
}
