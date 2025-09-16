/*******************

Team members and IDs:
Alex Jong A Kiem: 6361105
Meggan Raad: 6203012
Ericka Joseph: 6188612

*******************/

package net.floodlightcontroller.myrouting;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.linkdiscovery.LinkInfo;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.routing.RouteId;
import net.floodlightcontroller.staticflowentry.IStaticFlowEntryPusherService;
import net.floodlightcontroller.topology.NodePortTuple;

import org.openflow.util.HexString;
import org.openflow.util.U8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyRouting implements IOFMessageListener, IFloodlightModule {

	protected IFloodlightProviderService floodlightProvider;
	protected Set<Long> macAddresses;
	protected static Logger logger;
	protected IDeviceService deviceProvider;
	protected ILinkDiscoveryService linkProvider;

	protected Map<Long, IOFSwitch> switches;
	protected Map<Link, LinkInfo> links;
	protected Collection<? extends IDevice> devices;

	protected static int uniqueFlow;
	protected IStaticFlowEntryPusherService flowPusher;
	protected boolean printedTopo = false;
	
	
	protected Map<Long, ArrayList<AdjacencyTuple>> switchAdjacenyList = new HashMap<Long, ArrayList<AdjacencyTuple>>();
	 
	int networkSource = 0;
	int networkDestination = 0;
	
	//tuple class to store adjacent switch ID and Link
	class AdjacencyTuple{
		long switchID;
		Link link;
		
		public AdjacencyTuple(long switchID, Link link) {
			this.switchID = switchID;
			this.link = link;
		}
		public long getswitchID() {
			return switchID;
		}
		public Link getLink() {
			return link;
		}
		public String toString() {
			return Long.toString(switchID);
		}
		
	}
	
	
	
	
	@Override
	public String getName() {
		return MyRouting.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return (type.equals(OFType.PACKET_IN)
				&& (name.equals("devicemanager") || name.equals("topology")) || name
					.equals("forwarding"));
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IDeviceService.class);
		l.add(ILinkDiscoveryService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		deviceProvider = context.getServiceImpl(IDeviceService.class);
		linkProvider = context.getServiceImpl(ILinkDiscoveryService.class);
		flowPusher = context.getServiceImpl(IStaticFlowEntryPusherService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) 
	{
		
		// Get all switches in the network.
		switches = floodlightProvider.getAllSwitchMap();
		// Get all links in the network.
		links = linkProvider.getLinks();
		
		Map<Long, Set<Link>> switchLinks = linkProvider.getSwitchLinks();
		/*
		for(Map.Entry<Link,LinkInfo> link :links.entrySet()) {
			System.out.println("Switches: "+ link.getKey().getSrc()+ ", " + link.getKey().getDst());
			System.out.println("Ports: "+link.getKey().getSrcPort() + " " + link.getKey().getDstPort());
		}
		*/
		
		// QUESTION D
		// Print the topology if not yet.
		if (!printedTopo) {
			System.out.println("*** Print topology");
			

			// For each switch, print its neighbor switches.
			// ...
			//iterate through switches
			for(Map.Entry<Long, IOFSwitch> entry: switches.entrySet()) {
				//get the switch ID and print
				Long currentKey = entry.getKey();
				System.out.print("Switch "+ currentKey + " neighbors: ");
				
				//get links for current switch
				Set<Link> linkset = switchLinks.get(currentKey);
				/*
				for(Link link:linkset) {
					System.out.println(link.getDst());
				}
				*/
				
				//add switch and new adjacency array list to map (used in Dijkstra part)
				switchAdjacenyList.put(currentKey, new ArrayList<AdjacencyTuple>());
				
				//create string array of neighbor switch ID's so we can print them out
				ArrayList<String> neighbouringSwitches = new ArrayList<>();
				
				//iterate through the links and add neighbor switch ID's to the ArrayList
				for(Link link:linkset) {
					if(link.getDst()!=currentKey) {
						neighbouringSwitches.add(Long.toString(link.getDst()));
						//System.out.println(Long.toString(link.getDst()));
						//add adjacent switch to ArrayList (used for Dijkstra)
						switchAdjacenyList.get(currentKey).add(new AdjacencyTuple(link.getDst(), link));
					}
				}
				//print out neighbor switches
				System.out.println(String.join(",", neighbouringSwitches));
				
				
				
			}
			printedTopo = true;
		}


		// eth is the packet sent by a switch and received by floodlight.
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

		// We process only IP packets of type 0x0800.
		if (eth.getEtherType() != 0x0800) {
			
			return Command.CONTINUE;
		}
		else{
			

			// Parse the incoming packet.
			OFPacketIn pi = (OFPacketIn)msg;
			// The variable match contains source and destination IPs.
			OFMatch match = new OFMatch();
		    match.loadFromPacket(pi.getPacketData(), pi.getInPort());
		   
		   // System.out.println(match.getNetworkSource());
			
			
		    
	        boolean printRoute = false;
	        if((match.getNetworkSource()!= networkSource) || (match.getNetworkDestination()!=networkDestination)) {
	        	networkSource = match.getNetworkSource();
	        	networkDestination = match.getNetworkDestination();
	        	printRoute = true;
	        }

			// Calculate the path using Dijkstra's algorithm.
			Route route = null;
			// ...
			//get source switch
			long sourceSwitchID = findSwitchByIP(match.getNetworkSource());
			
			//get destination switch
			long destinationSwitchID = findSwitchByIP(match.getNetworkDestination());
			// data structure to store routes
			Map<Long, ArrayList<AdjacencyTuple>> routeMap = new HashMap<Long, ArrayList<AdjacencyTuple>>();
			// distance map
			Map<Long, Integer> distances  = new HashMap<Long, Integer>();
			
			
			// initialize default values
			for(Map.Entry<Long, Set<Link>> link:switchLinks.entrySet()) {
				routeMap.put(link.getKey(), new ArrayList<AdjacencyTuple>());
				distances.put(link.getKey(), Integer.MAX_VALUE);
			}
			
			
			Set<Long> explored = new HashSet<Long>();
			Set<Long> unsettled = new HashSet<Long>();
			//add source
			unsettled.add(sourceSwitchID);
			distances.put(sourceSwitchID, 0);
			
			//Dijkstra algorithm
			if(true) {
				System.out.println("*** New flow packet");
				// Print source and destination IPs.
				// ...
				System.out.println("srcIP: " + IPv4.fromIPv4Address(match.getNetworkSource()));
		        System.out.println("dstIP: " + IPv4.fromIPv4Address(match.getNetworkDestination()));
		       // System.out.println(sw.getId());
		        //System.out.println(match.getInputPort());
		        
				while(!unsettled.isEmpty()) {
					
					Long currentSwtcID = getLowestDistanceID(distances, unsettled);
					unsettled.remove(currentSwtcID);
					
					List<AdjacencyTuple> neighborList = switchAdjacenyList.get(currentSwtcID);
					//iterate through neighbors
					for(AdjacencyTuple neighbor : neighborList) {
						//update distance
						if(!explored.contains(neighbor.getswitchID()) && getLinkCost(currentSwtcID, neighbor.getswitchID()) + distances.get(currentSwtcID) < distances.get(neighbor.getswitchID())) {
							distances.put(neighbor.getswitchID(),getLinkCost(currentSwtcID, neighbor.getswitchID()) + distances.get(currentSwtcID));
							//copy path to source
							routeMap.put(neighbor.getswitchID(), new ArrayList<AdjacencyTuple>(routeMap.get(currentSwtcID)));
							routeMap.get(neighbor.getswitchID()).add(neighbor);
							unsettled.add(neighbor.getswitchID());
							
							
							
						}
						
					}
					explored.add(currentSwtcID);
					
					
				}
				System.out.println("route: " + sourceSwitchID+", "+routeMap.get(destinationSwitchID).toString().replace("[", "").replace("]", ""));
				//System.out.println("route: " + Arrays.toString(routeMap.get(destinationSwitchID).toArray()).replace("[", "").replace("]", ""));			

				// Write the path into the flow tables of the switches on the path.
			}
			
			// Install routing rules on switches. The path is a list of NodePortTuples,
			// and each switch on the path has two corresponding tuples (switch, inport)
			// and (switch, Outport).
			
			//get source host port IP
			short sourcePort = findPortByIP(match.getNetworkSource());
			short destinationPort = findPortByIP(match.getNetworkDestination());
			//convert route to NodePortTuple list
			List<NodePortTuple> switchPorts = new ArrayList<NodePortTuple>();
			
			
			//get route ArrayList
			
			List<AdjacencyTuple> routeList = routeMap.get(destinationSwitchID);
			
			//create route
			switchPorts.add(new NodePortTuple(sourceSwitchID, sourcePort));
			//System.out.println("RouteList Size: " + routeList.size());
			for(int l = 0; l<routeList.size();l++) {
				switchPorts.add(new NodePortTuple(routeList.get(l).getLink().getSrc(), routeList.get(l).getLink().getSrcPort()));
				switchPorts.add(new NodePortTuple(routeList.get(l).getLink().getDst(), routeList.get(l).getLink().getDstPort()));
			}
			switchPorts.add(new NodePortTuple(destinationSwitchID, destinationPort));
			
			/*
			for(int p = 0; p<switchPorts.size();p++) {
				System.out.println(p);
				System.out.println("Node ID: " + switchPorts.get(p).getNodeId());
				System.out.println("Port ID: " + switchPorts.get(p).getPortId());
				System.out.println("\n\n");
			}
			*/
			
			
			
			
			route = new Route(new RouteId(sourceSwitchID, destinationSwitchID), switchPorts);
			//create route for return path
			List<NodePortTuple> switchPortsReturn = new ArrayList<NodePortTuple>();
			switchPortsReturn.add(new NodePortTuple(destinationSwitchID, destinationPort));
			for(int j = routeList.size()-1; j>=0;j--) {
				
				switchPortsReturn.add(new NodePortTuple(routeList.get(j).getLink().getDst(), routeList.get(j).getLink().getDstPort()));
				switchPortsReturn.add(new NodePortTuple(routeList.get(j).getLink().getSrc(), routeList.get(j).getLink().getSrcPort()));
			}
			switchPortsReturn.add(new NodePortTuple(sourceSwitchID, sourcePort));
			
			/*
			for(int p = 0; p<switchPortsReturn.size();p++) {
				System.out.println("Return route: ");
				System.out.println(p);
				System.out.println("Node ID: " + switchPortsReturn.get(p).getNodeId());
				System.out.println("Port ID: " + switchPortsReturn.get(p).getPortId());
				System.out.println("\n\n");
			}
			*/
			
			Route returnRoute = new Route(new RouteId(destinationSwitchID, sourceSwitchID), switchPortsReturn);
			
			if (route != null) {
				installRoute(route.getPath(), match);
				installReturn(returnRoute.getPath(), match);
			}
			
			return Command.STOP;
		}
	}

	//method to get the link cost based on the rules given
	//
	//Both associated switches have odd IDs, e.g., (S1, S3) 1
	//Both associated switches have even IDs, e.g., (S2, S4) 100
	//Otherwise, e.g., (S1, S2) 10
	private int getLinkCost(Long a, Long b) {
		if(a%2==0 && b%2==0) {
			return 100;
		}
		//both uneven
		else if(a%2!=0 && b%2!=0) {
			return 1;
		}
		else {
			return 1;
		}
		
	}
	//method to to get lowest switch distance that is not in explored set
	private Long getLowestDistanceID(Map<Long, Integer> distances, Set<Long> unsettled) {
		int lowest = Integer.MAX_VALUE;
		Long lowestSwitchID = null;
		for(Long swtc:unsettled) {
			if(distances.get(swtc)< lowest) {
				lowest = distances.get(swtc);
				lowestSwitchID = swtc;
			}
		}
		
		return lowestSwitchID;
	}
	
	// Get switch id of switch that the host IP address is attached to
	private long findSwitchByIP(Integer ipv4Address) {
		// Find matching switches based on IP
		Iterator<? extends IDevice> deviceIterator = deviceProvider.queryDevices(null, null, ipv4Address, null, null);
		
		// Select first matching device
		if (deviceIterator.hasNext()) {
			IDevice device = deviceIterator.next();
			
			// Get device attachment points
			SwitchPort[] attachedSwitches = device.getAttachmentPoints();

			// Select first matching attachment point
			if (attachedSwitches.length >= 1) {
				return attachedSwitches[0].getSwitchDPID();
			}
		}
		return -1;
	}

	// Get port number of switch port that the host IP address is attached to
	private short findPortByIP(Integer ipv4Address) {
		// Find matching switches based on IP
		Iterator<? extends IDevice> deviceIterator = deviceProvider.queryDevices(null, null, ipv4Address, null, null);

		// Select first matching device
		if (deviceIterator.hasNext()) {
			IDevice device = deviceIterator.next();

			// Get device attachment points
			SwitchPort[] deviceSwitchPorts = device.getAttachmentPoints();

			// Select first matching attachment point
			if (deviceSwitchPorts.length >= 1) {
				return (short) deviceSwitchPorts[0].getPort();
			}
		}
		return -1;
	}

	// Install routing rules on switches. The path is a list of NodePortTuples,
	// and each switch on the path has two corresponding tuples (switch, inport)
	// and (switch, outport).
	private void installRoute(List<NodePortTuple> path, OFMatch match) {

		OFMatch m = new OFMatch();

		m.setDataLayerType(Ethernet.TYPE_IPv4)
				.setNetworkSource(match.getNetworkSource())
				.setNetworkDestination(match.getNetworkDestination());

		for (int i = 0; i <= path.size() - 1; i += 2) {
			short inport = path.get(i).getPortId();
			m.setInputPort(inport);
			List<OFAction> actions = new ArrayList<OFAction>();
			OFActionOutput outport = new OFActionOutput(path.get(i + 1)
					.getPortId());
			actions.add(outport);

			OFFlowMod mod = (OFFlowMod) floodlightProvider
					.getOFMessageFactory().getMessage(OFType.FLOW_MOD);
			mod.setCommand(OFFlowMod.OFPFC_ADD)
					.setIdleTimeout((short) 0)
					.setHardTimeout((short) 0)
					.setMatch(m)
					.setPriority((short) 105)
					.setActions(actions)
					.setLength(
							(short) (OFFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH));
			flowPusher.addFlow("routeFlow" + uniqueFlow, mod,
					HexString.toHexString(path.get(i).getNodeId()));
			uniqueFlow++;
		}
	}
	private void installReturn(List<NodePortTuple> path, OFMatch match) {

		OFMatch m = new OFMatch();

		m.setDataLayerType(Ethernet.TYPE_IPv4)
				.setNetworkSource(match.getNetworkDestination())
				.setNetworkDestination(match.getNetworkSource());

		for (int i = 0; i <= path.size() - 1; i += 2) {
			short inport = path.get(i).getPortId();
			m.setInputPort(inport);
			List<OFAction> actions = new ArrayList<OFAction>();
			OFActionOutput outport = new OFActionOutput(path.get(i + 1)
					.getPortId());
			actions.add(outport);

			OFFlowMod mod = (OFFlowMod) floodlightProvider
					.getOFMessageFactory().getMessage(OFType.FLOW_MOD);
			mod.setCommand(OFFlowMod.OFPFC_ADD)
					.setIdleTimeout((short) 0)
					.setHardTimeout((short) 0)
					.setMatch(m)
					.setPriority((short) 105)
					.setActions(actions)
					.setLength(
							(short) (OFFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH));
			flowPusher.addFlow("routeFlow" + uniqueFlow, mod,
					HexString.toHexString(path.get(i).getNodeId()));
			uniqueFlow++;
		}
	}
}
