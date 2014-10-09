package owltools.gaf.lego.server.handler;

public class PacketIdGenerator {

	private static int modCounter = 0;
	
	public synchronized static String generateId() {
		modCounter += 1;
		StringBuilder sb = new StringBuilder(Long.toHexString((System.nanoTime())));
		sb.append(Integer.toHexString(modCounter));
		// keep mod counter below 10000
		modCounter = modCounter % 10000;
		return sb.toString();
	}
	
}
