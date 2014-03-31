/**
 * 
 */
package com.jasperb.citybuilder.util;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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
            ViewGroup.LayoutParams layoutParams = mGridView.getLayoutParams();
            layoutParams.width = (int) (window.getDecorView().getWidth() * 0.8f);
            int numRowsVisibile = 4;
            layoutParams.height = (Constant.TILE_HEIGHT + 10)  * numRowsVisibile;
            mGridView.setLayoutParams(layoutParams);
            if (mType == TYPE_BUILDINGS) {
                mGridView.setNumColumns(TileBitmaps.getFullTileBitmaps().length);
                mGridView.setHorizontalScrollBarEnabled(true);
                mGridView.setVerticalScrollBarEnabled(false);
            } else {
                mGridView.setColumnWidth(Constant.TILE_WIDTH + 10);
                mGridView.setNumColumns(GridView.AUTO_FIT);
                mGridView.setHorizontalScrollBarEnabled(false);
                mGridView.setVerticalScrollBarEnabled(true);
            }

            if (mType == TYPE_BUILDINGS) {
                Bitmap[][] choiceBitmaps = ObjectBitmaps.mFullObjectBitmaps;
                ArrayAdapter<Bitmap[]> adapter = new ArrayAdapter<Bitmap[]>(getContext(), R.layout.grid_image_view, choiceBitmaps) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View row;

                        if (null == convertView) {
                            row = mInflater.inflate(R.layout.grid_image_view, null);
                        } else {
                            row = convertView;
                        }

                        ImageView iv = (ImageView) row.findViewById(R.id.grid_image_view);
                        iv.setImageBitmap(getItem(position)[0]);

                        return row;
                    }
                };

                mGridView.setAdapter(adapter);
            } else {
                Bitmap[] choiceBitmaps = TileBitmaps.getFullTileBitmaps();
                ArrayAdapter<Bitmap> adapter = new ArrayAdapter<Bitmap>(getContext(), R.layout.grid_image_view, choiceBitmaps) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View row;

                        if (null == convertView) {
                            row = mInflater.inflate(R.layout.grid_image_view, null);
                        } else {
                            row = convertView;
                        }

                        ImageView iv = (ImageView) row.findViewById(R.id.grid_image_view);
                        iv.setImageBitmap(getItem(position));

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
                    if(mType == TYPE_BUILDINGS) {
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
