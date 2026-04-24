package ui.views;

import model.SavedCard;
import storage.CardStorage;
import java.util.List;

// Displays the user's saved collection. All the layout and tile-building lives in
// SavedCardsView: this class just points the base at the collection storage methods.
public class CollectionView extends SavedCardsView {

    @Override
    protected String getTitle() {
        return "⭐ My Collection";
    }

    @Override
    protected String getEmptyMessage() {
        return "No cards in your collection yet. Search and add some!";
    }

    @Override
    protected List<SavedCard> loadCards() {
        return CardStorage.loadCollection();
    }

    @Override
    protected void removeCard(String id) {
        CardStorage.removeFromCollection(id);
    }
}