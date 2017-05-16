package com.example.lzanuzzo.tccapp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androidplot.xy.CatmullRomInterpolator;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ChartFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ChartFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChartFragment extends Fragment {
    private static final String ARG_XVALUES = "xvaluesparam";
    private static final String ARG_YVALUES = "yvaluesparam";
    private static final String ARG_GVALUES = "goalvalueparam";
    private static final String ARG_CVALUES = "minComsupvalueparam";

    private String xValuesParam;
    private String yValuesParam;
    private String goalParam;
    private String minComsupParam;
    private XYPlot plot;
    private String TAG = ChartFragment.class.getSimpleName();

    private OnFragmentInteractionListener mListener;

    public ChartFragment() {
        // Required empty public constructor
    }

    public static ChartFragment newInstance(String pxValuesParam, String pyValuesParam, String pGoalParam, String pMinConsump) {
        ChartFragment fragment = new ChartFragment();
        Bundle args = new Bundle();
        args.putString(ARG_XVALUES, pxValuesParam);
        args.putString(ARG_YVALUES, pyValuesParam);
        args.putString(ARG_GVALUES, pGoalParam);
        args.putString(ARG_CVALUES, pMinConsump);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            xValuesParam = getArguments().getString(ARG_XVALUES);
            yValuesParam = getArguments().getString(ARG_YVALUES);
            goalParam = getArguments().getString(ARG_GVALUES);
            minComsupParam = getArguments().getString(ARG_CVALUES);
        }
        Log.d(TAG,"onCreate Fragment");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.d(TAG,"onCreateView Fragment");

        View view = inflater.inflate(R.layout.fragment_chart, container, false);
        plot = (XYPlot) view.findViewById(R.id.plot);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        /*Number[] series2Numbers = {5, 2, 10, 5, 20, 10, 40, 20, 80, 40};*/

        String[] xSeries = xValuesParam.split(",");
        String[] ySeries = yValuesParam.split(",");
        ArrayList <Double> xSeriesDouble = new ArrayList<Double>();
        ArrayList <Double> ySeriesDouble = new ArrayList <Double>();
        ArrayList <Double> goalSeriesDouble = new ArrayList<Double>();
        ArrayList <Double> minConsumSeriesDouble = new ArrayList <Double>();

        for (int i=1;i < xSeries.length;i++)
        {
            try{
                goalSeriesDouble.add(Double.parseDouble(goalParam)*1000);
                minConsumSeriesDouble.add(Double.parseDouble(minComsupParam)*1000);
                xSeriesDouble.add(Double.parseDouble(xSeries[i]));
                ySeriesDouble.add(Double.parseDouble(ySeries[i])/1000);
            }catch (NumberFormatException e)
            {
                Log.e(TAG,"Some problem at: "+i+" position in the chart");
                Log.e(TAG,e.toString());
                e.printStackTrace();
            }
        }

        final Number[] domainLabels = new Number[xSeriesDouble.size()];
        Number[] VolumeNumbers = new Number[ySeriesDouble.size()];
        Number[] GoalNumbers = new Number[goalSeriesDouble.size()];
        Number[] ConsuNumbers = new Number[minConsumSeriesDouble.size()];
        for (int i=0;i < domainLabels.length;i++)
        {
            GoalNumbers[i] = goalSeriesDouble.get(i);
            ConsuNumbers[i] = minConsumSeriesDouble.get(i);
            domainLabels[i] = xSeriesDouble.get(i);
            VolumeNumbers[i] = ySeriesDouble.get(i);
        }

        XYSeries volumeSeries = new SimpleXYSeries(
                Arrays.asList(VolumeNumbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Volume");
        XYSeries goalSeries = new SimpleXYSeries(
                Arrays.asList(GoalNumbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Goal");
        XYSeries minConsuSeries = new SimpleXYSeries(
                Arrays.asList(ConsuNumbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Min. Cons.");

        LineAndPointFormatter volumeFormat =
                new LineAndPointFormatter(getContext(),R.xml.line_point_formatter_with_labels);

        LineAndPointFormatter goalFormat =
                new LineAndPointFormatter(getContext(),R.xml.line_point_formatter_with_labels_goal);

        LineAndPointFormatter minConsuFormat =
                new LineAndPointFormatter(getContext(),R.xml.line_point_formatter_with_labels_consum);


        /*volumeFormat.setInterpolationParams(
                new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Uniform));*/

        /*series2Format.setInterpolationParams(
                new CatmullRomInterpolator.Params(50, CatmullRomInterpolator.Type.Centripetal));*/

        // add a new series' to the xyplot:

        plot.addSeries(volumeSeries, volumeFormat);
        plot.addSeries(goalSeries, goalFormat);
        plot.addSeries(minConsuSeries, minConsuFormat);

        Paint fundo = new Paint();
        fundo.setARGB(0,0,0,0);
        plot.getGraph().setBackgroundPaint(fundo);
        plot.getGraph().getDomainCursorPaint().setARGB(0,0,0,0);

        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new Format() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                int i = Math.round(((Number) obj).floatValue());
                return toAppendTo.append("");
            }
            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return null;
            }
        });

    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Log.d(TAG,"onAttach Fragment");

        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
