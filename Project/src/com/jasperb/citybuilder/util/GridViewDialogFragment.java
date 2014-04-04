/**
 * 
 */
package com.jasperb.citybuilder.util;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jasperb.citybuilder.R;
import com.jasperb.citybuilder.util.Constant.OBJECTS;
import com.jasperb.citybuilder.util.Constant.TERRAIN;

/**
 * @author Jasper
 * 
 */
public class GridViewDialogFragment extends DialogFragment {
    public static final int TYPE_TILES = 0, TYPE_BUILDINGS = 1;
    public static final String TYPE = "Type";

    public interface GridViewDialogListener {
        public void onGridViewDialogAccept(int type, int selectedIndex);
    }

    private int mType;
    GridViewDialogListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mType = getArguments().getInt(TYPE);
        return new GridViewDialog(getActivity());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (GridViewDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement GridViewDialogListener");
        }
    }

    public class GridViewDialog extends Dialog implements android.view.View.OnClickListener {

        public static final int OBJECT_COL_WIDTH = 250;
        public static final int OBJECT_ROW_HEIGHT = 300;

        private Activity mActivity;
        private Button mAccept, mCancel;
        private GridView mGridView;
        private LayoutInflater mInflater;
        private int mSelectedIndex = -1;
        private TextView mTextDesc;

        public GridViewDialog(Activity a) {
            super(a);
            this.mActivity = a;
            mInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_NO_TITLE);

            setContentView(R.layout.fragment_grid_view_dialog);

            Rect displayRectangle = new Rect();
            Window window = mActivity.getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);

            mGridView = (GridView) findViewById(R.id.DialogGridView);

            //Snap to nearest row
            mGridView.setOnScrollListener(new OnScrollListener() {
                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}

                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    switch (scrollState) {
                    case OnScrollListener.SCROLL_STATE_IDLE:
                        GridView gridView = (GridView) view;
                        //Get the first visible child element of the grid view
                        //Determine where its top/bottom is relative to the top of the grid view
                        //Use that to determine whether to snap up or down
                        View itemView = view.getChildAt(0);
                        int top = itemView.getTop();
                        int bottom = itemView.getBottom();
                        int scrollBy = -top > bottom ? bottom : top;
                        if (scrollBy != 0)
                            gridView.smoothScrollBy(scrollBy, 10000); //Duration seems to be ignored?
                        break;
                    case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                    case OnScrollListener.SCROLL_STATE_FLING:
                        break;
                    }
                }
            });

            ViewGroup.LayoutParams layoutParams = mGridView.getLayoutParams();
            layoutParams.width = (int) (window.getDecorView().getWidth() * 0.8f);

            if (mType == TYPE_BUILDINGS) {
                layoutParams.width = layoutParams.width - (layoutParams.width % OBJECT_COL_WIDTH);
                int numColumns = layoutParams.width / OBJECT_COL_WIDTH;
                layoutParams.width += (numColumns - 1);
                mGridView.setNumColumns(numColumns);
                mGridView.setHorizontalSpacing(1);
                mGridView.setHorizontalScrollBarEnabled(false);
                mGridView.setVerticalScrollBarEnabled(true);

                layoutParams.height = OBJECT_ROW_HEIGHT;
            } else {
                mGridView.setColumnWidth(Constant.TILE_WIDTH + 10);
                mGridView.setNumColumns(GridView.AUTO_FIT);
                mGridView.setHorizontalScrollBarEnabled(false);
                mGridView.setVerticalScrollBarEnabled(true);

                int numRowsVisibile = 4;
                layoutParams.height = (Constant.TILE_HEIGHT + 10) * numRowsVisibile;
            }
            mGridView.setLayoutParams(layoutParams);

            if (mType == TYPE_BUILDINGS) {
                Bitmap[][] choiceBitmaps = ObjectBitmaps.getFullObjectBitmaps();
                ArrayAdapter<Bitmap[]> adapter = new ArrayAdapter<Bitmap[]>(getContext(), R.layout.grid_image_view, choiceBitmaps) {
                    Canvas mCanvas = new Canvas();
                    Matrix mMatrix = new Matrix();
                    Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        ImageView row;
                        Bitmap fullBitmap = null;

                        if (convertView == null) {
                            row = (ImageView) mInflater.inflate(R.layout.grid_image_view, null);
                        } else {
                            row = (ImageView) convertView;
                            BitmapDrawable d = (BitmapDrawable) row.getDrawable();
                            if (d != null)
                                fullBitmap = d.getBitmap();
                        }
                        if (fullBitmap == null) {
                            fullBitmap = Bitmap.createBitmap(OBJECT_COL_WIDTH, OBJECT_ROW_HEIGHT, Bitmap.Config.ARGB_8888);
                            row.setImageBitmap(fullBitmap);
                        }

                        int numSlices = ObjectBitmaps.getFullObjectBitmaps()[position].length;
                        int sliceWidth = ObjectBitmaps.getFullObjectBitmaps()[position][0].getWidth();
                        float imageWidth = (OBJECTS.objectNumRows[position] + OBJECTS.objectNumColumns[position])
                                * (Constant.TILE_WIDTH / 2);
                        if (OBJECT_COL_WIDTH < imageWidth) {
                            float scale = OBJECT_COL_WIDTH / imageWidth;
                            //Convert the scale to something that will ensure slices are not partially transparent on the left/right edges
                            scale = (float) (Math.floor(scale * (Constant.TILE_WIDTH / 2))) / (Constant.TILE_WIDTH / 2);
                            mMatrix.setScale(scale, scale);
                        } else {
                            mMatrix.reset();
                        }

                        fullBitmap.eraseColor(Color.TRANSPARENT);
                        mCanvas.setBitmap(fullBitmap);
                        for (int i = 0; i < numSlices; i++) {
                            mCanvas.drawBitmap(ObjectBitmaps.getFullObjectBitmaps()[position][i], mMatrix, mPaint);
                            mMatrix.preTranslate(sliceWidth, 0);
                        }
                        mPaint.setColor(Color.GRAY);

                        return row;
                    }
                };

                mGridView.setAdapter(adapter);
            } else {
                Bitmap[] choiceBitmaps = TileBitmaps.getFullTileBitmaps();
                ArrayAdapter<Bitmap> adapter = new ArrayAdapter<Bitmap>(getContext(), R.layout.grid_image_view, choiceBitmaps) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        ImageView row;

                        if (convertView == null) {
                            row = (ImageView) mInflater.inflate(R.layout.grid_image_view, null);
                        } else {
                            row = (ImageView) convertView;
                        }

                        row.setImageBitmap(getItem(position));

                        return row;
                    }
                };

                mGridView.setAdapter(adapter);
            }

            mTextDesc = (TextView) findViewById(R.id.DialogDesc);
            mAccept = (Button) findViewById(R.id.DialogAcceptButton);
            mCancel = (Button) findViewById(R.id.DialogCancelButton);
            mAccept.setOnClickListener(this);
            mCancel.setOnClickListener(this);

            mGridView.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    mSelectedIndex = (int) id;
                    if (mType == TYPE_BUILDINGS) {
                        mTextDesc.setText(OBJECTS.getName(mSelectedIndex));
                    } else {
                        mTextDesc.setText(TERRAIN.getName(mSelectedIndex));
                    }
                }
            });
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.DialogAcceptButton:
                if (mSelectedIndex >= 0) {
                    if (mListener != null)
                        mListener.onGridViewDialogAccept(mType, mSelectedIndex);
                    dismiss();
                } else {
                    Toast.makeText(getContext(), "Nothing Selected", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.DialogCancelButton:
                dismiss();
                break;
            default:
                break;
            }
        }
    }
}
