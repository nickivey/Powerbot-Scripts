import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.awt.*;

import org.powerbot.core.event.listeners.PaintListener;
import org.powerbot.core.script.ActiveScript;
import org.powerbot.core.script.job.Task;
import org.powerbot.core.script.job.state.Node;
import org.powerbot.core.script.job.state.Tree;
import org.powerbot.game.api.Manifest;
import org.powerbot.game.api.methods.Calculations;
import org.powerbot.game.api.methods.Game;
import org.powerbot.game.api.methods.Walking;
import org.powerbot.game.api.methods.interactive.NPCs;
import org.powerbot.game.api.methods.interactive.Players;
import org.powerbot.game.api.methods.node.GroundItems;
import org.powerbot.game.api.methods.tab.Inventory;
import org.powerbot.game.api.methods.widget.Bank;
import org.powerbot.game.api.methods.widget.Camera;
import org.powerbot.game.api.methods.widget.WidgetCache;
import org.powerbot.game.api.util.Random;
import org.powerbot.game.api.util.Time;
import org.powerbot.game.api.util.Timer;
import org.powerbot.game.api.wrappers.Area;
import org.powerbot.game.api.wrappers.Tile;
import org.powerbot.game.api.wrappers.interactive.NPC;
import org.powerbot.game.api.wrappers.node.GroundItem;
import org.powerbot.game.bot.Context;
import org.powerbot.game.client.Client;

@Manifest(authors = { "Warnick" }, name = "BurthCow", version = 1.0, description = "Kills cows in Burthorpe and Loots them")
public class BurthCow extends ActiveScript implements PaintListener{

	private Client client = Context.client();
	private Tree jobContainer = null;

	int CowsKilled;
	Timer runTime = new Timer(0);
	
	public Tile Bankc = new Tile(2892, 3530, 0);
	public Tile Fieldc = new Tile(2886,3488,0);
	public final Area Field = new Area(new Tile[] {
			new Tile(2891, 3492, 0), new Tile(2891, 3487, 0),
			new Tile(2892, 3482, 0), new Tile(2888, 3479, 0),
			new Tile(2883, 3479, 0), new Tile(2880, 3483, 0),
			new Tile(2879, 3488, 0), new Tile(2880, 3493, 0),
			new Tile(2881, 3498, 0) 
	});
	private final List<Node> jobsCollection = Collections
			.synchronizedList(new ArrayList<Node>());

	private final static int[] COW_ID = { 14997, 14999, 14998 };
	private final static int[] LOOT_ID = { 1739, 526, 2132 }; 

	public void onStart() {
		provide(new Attack(), new Banking(), new Pickup(), new WalktoField());
	}

	public boolean NeedToLoot() { 
		GroundItem loot = GroundItems.getNearest(LOOT_ID);
		while (loot != null) {
			return true;
		}
		return false;
		
	}
	public class Pickup extends Node {
		@Override
		public void execute() {
			System.out.println("Pickup Activated");
				GroundItem g = GroundItems.getNearest(LOOT_ID);
				if (g != null) {
					if (!g.isOnScreen()) {
						Walking.walk(g);
					} else {
						g.interact("Take", g.getGroundItem().getName());
						sleep(Random.nextInt(1200, 1500));
						if (Calculations.distanceTo(g.getLocation()) > 2) {
						}
					}
				}
			}
	

		@Override
		public boolean activate() {
			return NeedToLoot() && Field.contains(Players.getLocal().getLocation());
		}
	}
	
	public class WalktoField extends Node{

		@Override
		public boolean activate() {
			
			return !Field.contains(Players.getLocal().getLocation()) && !Inventory.isFull();
		}

		@Override
		public void execute() {
			System.out.println("Walk to field actviated");
			if (Players.getLocal().isIdle()) {
				Walking.walk(Fieldc);	
			}
			
			
		}
		
	}
	
	public class Banking extends Node {

		@Override
		public boolean activate() {
			return Inventory.isFull();
		}

		@Override
		public void execute() {
			System.out.println("Bank Activated");
			if (Bank.isOpen()) {
				if (Bank.depositInventory()) {
					Task.sleep(900, 1400);
					if (Bank.close()) {
						Task.sleep(800, 1000);

					}
				}
			} else if (!Bank.isOpen()) {
				if (Bank.getNearest() != null) {
					if (!Bank.getNearest().isOnScreen()) {
						if (Bankc != null) {
							Camera.turnTo(Bankc);
							Walking.walk(Bankc);
						}
					} else if (Bank.getNearest().isOnScreen()) {
						if (Bank.open()) {
							Task.sleep(800, 1000);
						}
					}
				}
			}
		}
	}

	public class Attack extends Node {

		@Override
		public boolean activate() {
			return Inventory.getCount() < 28
					&& !Players.getLocal().isInCombat() && Field.contains(Players.getLocal().getLocation());
		}

		@Override
		public void execute() {
		
			System.out.println("Attack Activated");
			NPC cow = NPCs.getNearest(COW_ID);
				if (cow.isOnScreen()) {
					if (!Players.getLocal().isInCombat()
							&& Players.getLocal().getInteracting() == null
							&& cow.getAnimation() == -1) {
						cow.interact("Attack");
						sleep(Random.nextInt(1500, 2000));
						CowsKilled++;
					}
				} else {
					Walking.walk(cow.getLocation());
					Camera.setPitch(true);
				}
			}
	}

	@Override
	public int loop() {
		if (Game.getClientState() != Game.INDEX_MAP_LOADED) {
			return 1000;
		}

		if (client != Context.client()) {
			WidgetCache.purge();
			Context.get().getEventManager().addListener(this);
			client = Context.client();
		}
		if (jobContainer != null) {
			final Node job = jobContainer.state();
			if (job != null) {
				jobContainer.set(job);
				getContainer().submit(job);
				job.join();
			}
		}
		return 50;
	}

	public final void provide(final Node... jobs) {
		for (final Node job : jobs) {
			if (!jobsCollection.contains(job)) {
				jobsCollection.add(job);
			}
		}
		jobContainer = new Tree(jobsCollection.toArray(new Node[jobsCollection
				.size()]));
	}
	    	private final RenderingHints antialiasing = new RenderingHints(
	                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

	        private final Color color1 = new Color(255, 0, 0);

	        private final Font font1 = new Font("Arial", 0, 26);
	        private final Font font2 = new Font("Arial", 0, 19);

	        public void onRepaint(Graphics g1) {
	            Graphics2D g = (Graphics2D)g1;
	            g.setRenderingHints(antialiasing);

	            g.setFont(font1);
	            g.setColor(color1);
	            g.drawString("Nicks Cow Killa", 18, 425);
	            g.setFont(font2);
	            g.drawString("Total Cows Killed: "+CowsKilled, 107, 459);
	            g.drawString("Time Runnning: "+ Time.format(runTime.getElapsed()), 106, 486);
	        }
	        //END: Generated By OmniEasel

		}
	
	
    

