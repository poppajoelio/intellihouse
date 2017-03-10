package house.intelli.core.jaxb;

import java.util.HashSet;
import java.util.Set;

import house.intelli.core.rpc.DeferredResponseRequest;
import house.intelli.core.rpc.DeferringResponse;
import house.intelli.core.rpc.ErrorResponse;
import house.intelli.core.rpc.RpcService;
import house.intelli.core.rpc.RpcServiceRegistry;
import house.intelli.core.rpc.NullResponse;

public class IntelliHouseJaxbContextProviderImpl extends AbstractIntelliHouseJaxbContextProvider {

	@Override
	public Class<?>[] getClassesToBeBound() {
		Set<Class<?>> classes = new HashSet<>();

		// automatically enlist all Request and Response sub-classes used by the RpcServices.
		for (RpcService<?, ?> rpcService : RpcServiceRegistry.getInstance().createRpcServices()) {
			classes.add(rpcService.getRequestType());
			classes.add(rpcService.getResponseType());
		}

		// manually add other classes below...
		classes.add(DeferredResponseRequest.class);
		classes.add(DeferringResponse.class);
		classes.add(ErrorResponse.class);
		classes.add(NullResponse.class);

		return classes.toArray(new Class<?>[classes.size()]);
	}

}
