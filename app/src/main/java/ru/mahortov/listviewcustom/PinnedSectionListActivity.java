
package ru.mahortov.listviewcustom;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import ru.mahortov.listviewcustom.ListView.PinnedSectionListView;
import ru.mahortov.listviewcustom.ListView.PinnedSectionListView.PinnedSectionListAdapter;
import ru.mahortov.listviewcustom.SwipeMenu.SwipeMenu;
import ru.mahortov.listviewcustom.SwipeMenu.SwipeMenuCreator;
import ru.mahortov.listviewcustom.SwipeMenu.SwipeMenuItem;

import static ru.mahortov.listviewcustom.R.*;

public class PinnedSectionListActivity extends ListActivity implements OnClickListener {

    PinnedSectionListView pinnedSectionListView;

    static class SimpleAdapter extends ArrayAdapter<Item> implements PinnedSectionListAdapter {

        private static final int[] COLORS = new int[] {
            color.blue_light, color.red_light, color.orange_light, color.green_light,
                color.dark_grey};

        public SimpleAdapter(Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);
            generateDataset('A', 'O', false);
        }

        public void generateDataset(char from, char to, boolean clear) {
        	
        	if (clear) clear();
        	
            final int sectionsNumber = to - from + 1;
            prepareSections(sectionsNumber);

            int sectionPosition = 0, listPosition = 0;
            for (char i=0; i<sectionsNumber; i++) {
                Item section = new Item(Item.SECTION, String.valueOf((char)(from + i)));
                section.sectionPosition = sectionPosition;
                section.listPosition = listPosition++;
                onSectionAdded(section, sectionPosition);
                add(section);

                final int itemsNumber = (int) Math.abs((Math.cos(2f*Math.PI/3f * sectionsNumber / (i+1f)) * 25f));
                for (int j=0;j<itemsNumber;j++) {
                    Item item = new Item(Item.ITEM, section.text.toUpperCase(Locale.ENGLISH) + " - " + j);
                    item.sectionPosition = sectionPosition;
                    item.listPosition = listPosition++;
                    add(item);
                }

                sectionPosition++;
            }
        }
        
        protected void prepareSections(int sectionsNumber) { }
        protected void onSectionAdded(Item section, int sectionPosition) { }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getView(position, convertView, parent);
            view.setTextColor(Color.DKGRAY);
            view.setTag("" + position);
            Item item = getItem(position);
            if (item.type == Item.SECTION) {
                //view.setOnClickListener(PinnedSectionListActivity.this);
                view.setBackgroundColor(parent.getResources().getColor(COLORS[item.sectionPosition % COLORS.length]));
            }
            return view;
        }

        @Override public int getViewTypeCount() {
            return 2;
        }

        @Override public int getItemViewType(int position) {
            return getItem(position).type;
        }

        @Override
        public boolean isItemViewTypePinned(int viewType) {
            return viewType == Item.SECTION;
        }

    }

    static class FastScrollAdapter extends SimpleAdapter implements SectionIndexer {

        private Item[] sections;

        public FastScrollAdapter(Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);
        }

        @Override protected void prepareSections(int sectionsNumber) {
            sections = new Item[sectionsNumber];
        }

        @Override protected void onSectionAdded(Item section, int sectionPosition) {
            sections[sectionPosition] = section;
        }

        @Override public Item[] getSections() {
            return sections;
        }

        @Override public int getPositionForSection(int section) {
            if (section >= sections.length) {
                section = sections.length - 1;
            }
            return sections[section].listPosition;
        }

        @Override public int getSectionForPosition(int position) {
            if (position >= getCount()) {
                position = getCount() - 1;
            }
            return getItem(position).sectionPosition;
        }

    }

	static class Item {

		public static final int ITEM = 0;
		public static final int SECTION = 1;

		public final int type;
		public final String text;

		public int sectionPosition;
		public int listPosition;

		public Item(int type, String text) {
		    this.type = type;
		    this.text = text;
		}

		@Override public String toString() {
			return text;
		}

	}

	private boolean hasHeaderAndFooter;
	private boolean isFastScroll;
	private boolean addPadding;
	private boolean isShadowVisible = true;
	private int mDatasetUpdateCount;
    SwipeRefreshLayout mSwipeRefreshLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(layout.activity_main);
        pinnedSectionListView = (PinnedSectionListView) getListView();

		if (savedInstanceState != null) {
		    isFastScroll = savedInstanceState.getBoolean("isFastScroll");
		    addPadding = savedInstanceState.getBoolean("addPadding");
		    isShadowVisible = savedInstanceState.getBoolean("isShadowVisible");
		    hasHeaderAndFooter = savedInstanceState.getBoolean("hasHeaderAndFooter");
		}
		initializeHeaderAndFooter();
		initializeAdapter();
		initializePadding();
	}

    @Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
	    super.onSaveInstanceState(outState);
	    outState.putBoolean("isFastScroll", isFastScroll);
	    outState.putBoolean("addPadding", addPadding);
	    outState.putBoolean("isShadowVisible", isShadowVisible);
	    outState.putBoolean("hasHeaderAndFooter", hasHeaderAndFooter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
	    Item item = (Item) getListView().getAdapter().getItem(position);
	    if (item != null) {
	        Toast.makeText(this, "Item " + position + ": " + item.text, Toast.LENGTH_SHORT).show();
	    } else {
	        Toast.makeText(this, "Item " + position, Toast.LENGTH_SHORT).show();
	    }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		menu.getItem(0).setChecked(isFastScroll);
		menu.getItem(1).setChecked(addPadding);
		menu.getItem(2).setChecked(isShadowVisible);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
    	    case id.action_fastscroll:
    	        isFastScroll = !isFastScroll;
    	        item.setChecked(isFastScroll);
    	        initializeAdapter();
    	        break;
    	    case id.action_addpadding:
    	        addPadding = !addPadding;
    	        item.setChecked(addPadding);
    	        initializePadding();
    	        break;
    	    case id.action_showShadow:
    	        isShadowVisible = !isShadowVisible;
    	        item.setChecked(isShadowVisible);
    	        ((PinnedSectionListView)getListView()).setShadowVisible(isShadowVisible);
    	        break;
    	    case id.action_showHeaderAndFooter:
    	        hasHeaderAndFooter = !hasHeaderAndFooter;
    	        item.setChecked(hasHeaderAndFooter);
    	        initializeHeaderAndFooter();
    	        break;
    	    case id.action_updateDataset:
    	    	updateDataset();
    	    	break;
	    }
	    return true;
	}

	private void updateDataset() {
		mDatasetUpdateCount++;
		SimpleAdapter adapter = (SimpleAdapter) getListAdapter();
		switch (mDatasetUpdateCount % 4) {
			case 0: adapter.generateDataset('A', 'B', true); break;
			case 1: adapter.generateDataset('C', 'D', true); break;
			case 2: adapter.generateDataset('E', 'L', true); break;
			case 3: adapter.generateDataset('X', 'Z', true); break;
		}
		adapter.notifyDataSetChanged();
	}
	
	private void initializePadding() {
	    float density = getResources().getDisplayMetrics().density;
	    int padding = addPadding ? (int) (16 * density) : 0;
	    getListView().setPadding(padding, padding, padding, padding);
	}

    private void initializeHeaderAndFooter() {
        setListAdapter(null);
        if (hasHeaderAndFooter) {
            ListView list = getListView();

            LayoutInflater inflater = LayoutInflater.from(this);
            TextView header1 = (TextView) inflater.inflate(android.R.layout.simple_list_item_1, list, false);
            header1.setText("First header");
            list.addHeaderView(header1);

            TextView header2 = (TextView) inflater.inflate(android.R.layout.simple_list_item_1, list, false);
            header2.setText("Second header");
            list.addHeaderView(header2);

            TextView footer = (TextView) inflater.inflate(android.R.layout.simple_list_item_1, list, false);
            footer.setText("Single footer");
            list.addFooterView(footer);
        }
        initializeAdapter();
    }

    @SuppressLint("NewApi")
    private void initializeAdapter() {
        getListView().setFastScrollEnabled(isFastScroll);
        if (isFastScroll) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                getListView().setFastScrollAlwaysVisible(true);
            }
            setListAdapter(new FastScrollAdapter(this, android.R.layout.simple_list_item_1, android.R.id.text1));
        } else {
            setListAdapter(new SimpleAdapter(this, android.R.layout.simple_list_item_1, android.R.id.text1));
        }
        SwipeMenuCreator creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {
                // create "open" item
                if (menu.getViewType() != 1) {
                    SwipeMenuItem openItem = new SwipeMenuItem(
                            getApplicationContext());
                    // set item background
                    openItem.setBackground(new ColorDrawable(Color.rgb(0xC9, 0xC9,
                            0xCE)));
                    // set item width
                    openItem.setWidth(dp2px(90));
                    // set item title
                    openItem.setTitle("Open");
                    // set item title fontsize
                    openItem.setTitleSize(18);
                    // set item title font color
                    openItem.setTitleColor(Color.WHITE);
                    // add to menu
                    menu.addMenuItem(openItem);

                    // create "delete" item
                    SwipeMenuItem deleteItem = new SwipeMenuItem(
                            getApplicationContext());
                    // set item background
                    deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9,
                            0x3F, 0x25)));
                    // set item width
                    deleteItem.setWidth(dp2px(90));
                    // set a icon
                    deleteItem.setTitle("Delete");
                    // set item title fontsize
                    deleteItem.setTitleSize(18);
                    // set item title font color
                    deleteItem.setTitleColor(Color.WHITE);
                    // add to menu
                    menu.addMenuItem(deleteItem);
                }
            }
        };
        // set creator
        pinnedSectionListView.setMenuCreator(creator);

        // step 2. listener item click event
        pinnedSectionListView.setOnMenuItemClickListener(new PinnedSectionListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                Object item = getListAdapter().getItem(position);
                switch (index) {
                    case 0:
                        // open
                        Toast.makeText(getApplicationContext(), "open " + item.toString(), Toast.LENGTH_LONG).show();
                        break;
                    case 1:
                        // delete
//					delete(item);
                        Toast.makeText(getApplicationContext(), "delete " + item.toString(), Toast.LENGTH_LONG).show();
                        break;
                }
                return false;
            }
        });

        // set SwipeListener
        pinnedSectionListView.setOnSwipeListener(new PinnedSectionListView.OnSwipeListener() {

            @Override
            public void onSwipeStart(int position) {
                // swipe start
            }

            @Override
            public void onSwipeEnd(int position) {
                // swipe end
            }
        });

        // other setting
//		listView.setCloseInterpolator(new BounceInterpolator());

        // test item long click
        pinnedSectionListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int position, long id) {

//                View customToastroot = getLayoutInflater().inflate(R.layout.custom_toast, null);
//                TextView textView = (TextView) customToastroot.findViewById(R.id.textView1);
//                textView.setText(mAppList.get(position).loadLabel(getPackageManager()));
//
//                Toast customtoast = new Toast(getApplicationContext());
//
//                customtoast.setView(customToastroot);
//                customtoast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
//                customtoast.setDuration(Toast.LENGTH_LONG);
//                customtoast.show();
                Toast.makeText(getApplicationContext(), parent.getItemAtPosition(position).toString(), Toast.LENGTH_LONG).show();
                return false;
            }
        });
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }


    @Override
    public void onClick(View v) {
        Toast.makeText(this, "Item: " + v.getTag() , Toast.LENGTH_SHORT).show();
    }

}