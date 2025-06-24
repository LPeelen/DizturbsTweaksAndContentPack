package dtcp;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.impl.campaign.submarkets.OpenMarketPlugin;
import org.apache.log4j.Logger;

@SuppressWarnings("unused")
public class DTCPModPlugin extends BaseModPlugin {

    private static final Logger LOGGER = Global.getLogger(DTCPModPlugin.class);

    @Override
    public void onApplicationLoad() throws Exception {
        super.onApplicationLoad();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        Global.getSector().getListenerManager().addListener(new ColonyInteractionListener() {
            @Override
            public void reportPlayerOpenedMarket(MarketAPI market) {
                nerfMarket(market);
            }

            @Override
            public void reportPlayerClosedMarket(MarketAPI market) {}

            @Override
            public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) {}

            @Override
            public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {}
        });

        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            nerfMarket(market);
        }
    }

    private void nerfMarket(MarketAPI market) {
        String marketName = market.getName();
        SubmarketAPI vanillaOpenMarket = market.getSubmarket(Submarkets.SUBMARKET_OPEN);

        if (vanillaOpenMarket == null || isNerfed(vanillaOpenMarket)) {
            LOGGER.info(new StringBuilder("Skipping already nerfed open market at: ").append(marketName));
            return;
        }

        LOGGER.info(new StringBuilder("Nerfing open market at: ").append(marketName));

        // Remove and add the submarket so that it uses the nerfed open market plugin.
        market.removeSubmarket(Submarkets.SUBMARKET_OPEN);
        market.addSubmarket(Submarkets.SUBMARKET_OPEN);

        // Copy the vanilla submarket cargo to the nerfed submarket ship catalog.
        // Actual filtering of legal items and ships is handled by the nerfed open market plugin.
        SubmarketAPI nerfedOpenMarket = market.getSubmarket(Submarkets.SUBMARKET_OPEN);
        SubmarketPlugin vanillaPlugin = vanillaOpenMarket.getPlugin();
        SubmarketPlugin nerfedPlugin = nerfedOpenMarket.getPlugin();

        nerfedPlugin.getCargo().clear();
        for (FleetMemberAPI ship : vanillaPlugin.getCargo().getMothballedShips().getMembersListCopy()) {
            nerfedPlugin.getCargo().getMothballedShips().addFleetMember(ship);
        }

        for (CargoStackAPI cargoStack : vanillaPlugin.getCargo().getStacksCopy()) {
            nerfedPlugin.getCargo().addFromStack(cargoStack);
        }

        // Copy the state of the vanilla submarket to the nerfed submarket.
        // This requires downcasting to BaseSubmarketPlugin to access the required setter methods.
        if (vanillaPlugin instanceof BaseSubmarketPlugin vanillaBasePlugin
                && nerfedPlugin instanceof BaseSubmarketPlugin nerfedBasePlugin) {
            nerfedBasePlugin.setMinSWUpdateInterval(vanillaBasePlugin.getMinSWUpdateInterval());
            nerfedBasePlugin.setSinceLastCargoUpdate(vanillaBasePlugin.getSinceLastCargoUpdate());
            nerfedBasePlugin.setSinceSWUpdate(vanillaBasePlugin.getSinceSWUpdate());
        }
    }


    private boolean isNerfed(SubmarketAPI subMarket) {
        String pluginName = subMarket.getPlugin().getClass().getSimpleName();
        return !pluginName.equals(OpenMarketPlugin.class.getSimpleName());
    }
}