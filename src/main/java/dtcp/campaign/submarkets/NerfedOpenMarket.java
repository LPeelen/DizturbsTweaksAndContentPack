package dtcp.campaign.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.OpenMarketPlugin;
import dtcp.util.Setting;
import org.apache.log4j.Logger;

import java.util.EnumSet;
import java.util.List;

public class NerfedOpenMarket extends OpenMarketPlugin {

    public static final Setting<List<String>> LEGAL_SHIP_KEYWORDS = new Setting<>("legalShipKeywords", List.of(), String.class);
    public static final Setting<List<String>> LEGAL_ITEM_TYPES = new Setting<>("legalItemTypes", List.of(), String.class);
    private static final Logger LOGGER = Global.getLogger(NerfedOpenMarket.class);

    @Override
    public void init(SubmarketAPI submarket) {
        super.init(submarket);
    }

    @Override
    public void updateCargoPrePlayerInteraction() {
        super.updateCargoPrePlayerInteraction();

        FleetDataAPI shipsForSale = submarket.getCargo().getMothballedShips();

        // If the list of keywords is empty for whatever reason then don't do anything.
        // Otherwise, nothing will be sold on the open market.
        if (LEGAL_SHIP_KEYWORDS.get().isEmpty()) {
            return;
        }

        for (FleetMemberAPI ship : shipsForSale.getMembersListCopy()) {
            ShipHullSpecAPI hull =
                    (ship.getHullSpec().getBaseHull() != null) ? ship.getHullSpec().getBaseHull() : ship.getHullSpec();

            // The conditions inside 'anyMatch' are order from most to least likely to be truthy.
            // NOTE: For reasons unknown to me. Hints of some ships that are found inside ship_data.csv are-
            // accessed via 'getTags()' instead of 'getHints()'.
            boolean isLegal = LEGAL_SHIP_KEYWORDS.get()
                    .stream()
                    .anyMatch(keyword -> isInHintSet(keyword, hull.getHints())
                            || hull.getTags().contains(keyword)
                            || keyword.equals(hull.getDesignation())
                            || keyword.equals(hull.getHullName()));

            if (isLegal) {
                continue;
            }

            LOGGER.info("Removing ship '" + ship.getHullId() + "' from open market at '"
                    + submarket.getMarket().getName());

            shipsForSale.removeFleetMember(ship);
        }

        List<CargoStackAPI> cargoStacks = submarket.getCargo().getStacksCopy();
        for (CargoStackAPI cargoStack : cargoStacks) {
            boolean isLegal = LEGAL_ITEM_TYPES.get()
                    .stream()
                    .anyMatch(type -> cargoStack.getType().name().equalsIgnoreCase(type));

            if (isLegal) {
                continue;
            }

            LOGGER.info("Removing item '" + cargoStack.getDisplayName() + "' from open market at '"
                    + submarket.getMarket().getName());

            submarket.getCargo().removeStack(cargoStack);
        }
    }

    private boolean isInHintSet(String keyword, EnumSet<ShipHullSpecAPI.ShipTypeHints> hintSet) {
        for (ShipHullSpecAPI.ShipTypeHints hint : hintSet) {
            if (hint.name().equalsIgnoreCase(keyword)) {
                return true;
            }
        }
        return false;
    }
}
