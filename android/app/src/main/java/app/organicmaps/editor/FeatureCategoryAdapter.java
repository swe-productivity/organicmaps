package app.organicmaps.editor;

import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import app.organicmaps.R;
import app.organicmaps.sdk.editor.data.FeatureCategory;
import app.organicmaps.sdk.util.StringUtils;
import app.organicmaps.util.UiUtils;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class FeatureCategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
  private static final int TYPE_HEADER = 0;
  private static final int TYPE_CATEGORY = 1;
  private static final int TYPE_FOOTER = 2;

  // A display item is either a header (String) or a category (FeatureCategory).
  private List<Object> mItems = new ArrayList<>();
  private final FeatureCategoryFragment mFragment;
  private final FeatureCategory mSelectedCategory;

  public interface FooterListener
  {
    void onNoteTextChanged(String newText);
    void onSendNoteClicked();
  }

  public FeatureCategoryAdapter(@NonNull FeatureCategoryFragment host,
                                @NonNull FeatureCategory[] recentCategories,
                                @NonNull FeatureCategory[] allCategories,
                                @Nullable FeatureCategory selectedCategory)
  {
    mFragment = host;
    mSelectedCategory = selectedCategory;
    mItems = buildDisplayList(recentCategories, allCategories);
  }

  /**
   * Builds the mixed display list:
   *   [Header "Recently Used"] [recent items...]
   *   [Header "All Types"]     [all items (excluding recents)...]
   * If recentCategories is empty, no "Recently Used" section is shown.
   */
  @NonNull
  private List<Object> buildDisplayList(@NonNull FeatureCategory[] recentCategories,
                                        @NonNull FeatureCategory[] allCategories)
  {
    List<Object> items = new ArrayList<>();

    if (recentCategories.length > 0)
    {
      items.add(mFragment.getString(R.string.editor_recently_used_types));
      for (FeatureCategory c : recentCategories)
        items.add(c);
      items.add(mFragment.getString(R.string.editor_all_types));
    }

    // Build a set of recent type keys to exclude from the "All" section
    java.util.Set<String> recentKeys = new java.util.HashSet<>();
    for (FeatureCategory c : recentCategories)
      recentKeys.add(c.getType());

    for (FeatureCategory c : allCategories)
    {
      if (!recentKeys.contains(c.getType()))
        items.add(c);
    }

    return items;
  }

  /**
   * Called when showing search results — flat list, no section headers.
   */
  public void setSearchResults(@NonNull FeatureCategory[] categories)
  {
    mItems = new ArrayList<>();
    for (FeatureCategory c : categories)
      mItems.add(c);
    notifyDataSetChanged();
  }

  /**
   * Called when restoring the full sectioned view (search cleared).
   */
  public void setSectionedCategories(@NonNull FeatureCategory[] recentCategories,
                                     @NonNull FeatureCategory[] allCategories)
  {
    mItems = buildDisplayList(recentCategories, allCategories);
    notifyDataSetChanged();
  }

  @Override
  public int getItemViewType(int position)
  {
    if (position == mItems.size())
      return TYPE_FOOTER;
    Object item = mItems.get(position);
    if (item instanceof String)
      return TYPE_HEADER;
    return TYPE_CATEGORY;
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
  {
    final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    switch (viewType)
    {
    case TYPE_HEADER ->
    {
      return new HeaderViewHolder(inflater.inflate(R.layout.item_feature_category_header, parent, false));
    }
    case TYPE_CATEGORY ->
    {
      return new FeatureViewHolder(inflater.inflate(R.layout.item_feature_category, parent, false));
    }
    case TYPE_FOOTER ->
    {
      return new FooterViewHolder(inflater.inflate(R.layout.item_feature_category_footer, parent, false),
                                  (FooterListener) mFragment);
    }
    default ->
    {
      throw new IllegalArgumentException("Unsupported viewType: " + viewType);
    }
    }
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
  {
    if (holder instanceof HeaderViewHolder)
      ((HeaderViewHolder) holder).bind((String) mItems.get(position));
    else if (holder instanceof FeatureViewHolder)
      ((FeatureViewHolder) holder).bind((FeatureCategory) mItems.get(position));
    else if (holder instanceof FooterViewHolder)
      ((FooterViewHolder) holder).bind(mFragment.getPendingNoteText());
  }

  @Override
  public int getItemCount()
  {
    return mItems.size() + 1; // +1 for footer
  }

  protected static class HeaderViewHolder extends RecyclerView.ViewHolder
  {
    @NonNull
    private final TextView mTitle;

    HeaderViewHolder(@NonNull View itemView)
    {
      super(itemView);
      mTitle = itemView.findViewById(R.id.header_title);
    }

    public void bind(@NonNull String title)
    {
      mTitle.setText(title);
    }
  }

  protected class FeatureViewHolder extends RecyclerView.ViewHolder
  {
    @NonNull
    private final TextView mName;
    @NonNull
    private final View mSelected;

    FeatureViewHolder(@NonNull View itemView)
    {
      super(itemView);
      mName = itemView.findViewById(R.id.name);
      mSelected = itemView.findViewById(R.id.selected);
      UiUtils.hide(mSelected);
      itemView.setOnClickListener(v -> {
        int pos = getBindingAdapterPosition();
        if (pos != RecyclerView.NO_ID && mItems.get(pos) instanceof FeatureCategory)
          mFragment.selectCategory((FeatureCategory) mItems.get(pos));
      });
    }

    public void bind(@NonNull FeatureCategory category)
    {
      mName.setText(category.getLocalizedTypeName());
      boolean showCondition =
          mSelectedCategory != null && category.getType().equals(mSelectedCategory.getType());
      UiUtils.showIf(showCondition, mSelected);
    }
  }

  protected static class FooterViewHolder extends RecyclerView.ViewHolder
  {
    private final TextInputEditText mNoteEditText;
    private final View mSendNoteButton;

    FooterViewHolder(@NonNull View itemView, @NonNull FooterListener listener)
    {
      super(itemView);
      TextView categoryUnsuitableText = itemView.findViewById(R.id.editor_category_unsuitable_text);
      categoryUnsuitableText.setMovementMethod(LinkMovementMethod.getInstance());
      mNoteEditText = itemView.findViewById(R.id.note_edit_text);
      mSendNoteButton = itemView.findViewById(R.id.send_note_button);
      mSendNoteButton.setOnClickListener(v -> listener.onSendNoteClicked());
      mNoteEditText.addTextChangedListener(new StringUtils.SimpleTextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count)
        {
          final String str = s.toString();
          listener.onNoteTextChanged(str);
          mSendNoteButton.setEnabled(!str.trim().isEmpty());
        }
      });
    }

    public void bind(String pendingNoteText)
    {
      if (!mNoteEditText.getText().toString().equals(pendingNoteText))
      {
        mNoteEditText.setText(pendingNoteText);
        if (pendingNoteText != null)
          mNoteEditText.setSelection(pendingNoteText.length());
      }
      mSendNoteButton.setEnabled(pendingNoteText != null && !pendingNoteText.trim().isEmpty());
    }
  }
}
