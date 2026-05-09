package ui.views;

import model.SavedCard;
import storage.CardStorage;
import java.util.List;

// Displays the user's wishlist. All the layout and tile-building lives in
// SavedCardsView - this class just points the base at the wishlist storage methods.
public class WishlistView extends SavedCardsView {

    @Override
    protected String getTitle() {
        return "My Wishlist";
    }

    @Override
    protected String getEmptyMessage() {
        return "Your wishlist is empty. Find cards and add them!";
    }

    @Override
    protected List<SavedCard> loadCards() {
        return CardStorage.loadWishlist();
    }

    @Override
    protected void removeCard(String id) {
        CardStorage.removeFromWishlist(id);
    }
}

