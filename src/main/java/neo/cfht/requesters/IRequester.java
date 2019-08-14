package neo.cfht.requesters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import neo.cfht.models.SmallBodyRequest;
import neo.exceptions.NeoIOException;

public interface IRequester extends Callable<IRequester> {

	boolean isRequestSuccessful();
	
	public static List<IRequester> getRequesters(SmallBodyRequest smallBodyRequest) {
		List<IRequester> requesters = new ArrayList<>();
		requesters.add(new JplNeoRequester(smallBodyRequest));
		requesters.add(new MpcKnownRequester(smallBodyRequest));
		return requesters;
	}

	String getCfhtXML();

	void write() throws NeoIOException;

	String getOutputFileName();
}
