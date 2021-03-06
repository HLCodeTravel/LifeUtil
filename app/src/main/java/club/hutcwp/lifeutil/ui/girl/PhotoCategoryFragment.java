package club.hutcwp.lifeutil.ui.girl;

import android.graphics.Rect;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.View;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import club.hutcwp.lifeutil.R;
import club.hutcwp.lifeutil.adpter.PhotoAdapter;
import club.hutcwp.lifeutil.databinding.FragmentPhotoCategoryBinding;
import club.hutcwp.lifeutil.model.PhotoItem;
import club.hutcwp.lifeutil.ui.MainActivity;
import club.hutcwp.lifeutil.ui.base.BaseFragment;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class PhotoCategoryFragment extends BaseFragment {

    private Subscription subscription;
    List<PhotoItem> photoItems = new ArrayList<>();
    PhotoAdapter adapter;
    FragmentPhotoCategoryBinding binding;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_photo_category;
    }

    @Override
    protected void initViews() {

        binding = (FragmentPhotoCategoryBinding) getBinding();
        adapter = new PhotoAdapter(getContext(),photoItems);
        setting();

    }

    @Override
    protected void lazyFetchData() {

        getDataFromServer();
    }

    /**
     * 设置监听等
     */
    public void setting() {

        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        binding.recyclerView.addItemDecoration(new SpacesItemDecoration(24));
        binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                getDataFromServer();
            }
        });

    }

    /**
     * 从服务器上获取数据
     */
    public void getDataFromServer() {


        final String url =  getArguments().getString("url");

        subscription = Observable.just(url).subscribeOn(Schedulers.io()).map(new Func1<String, List<PhotoItem>>() {
            @Override
            public List<PhotoItem> call(String s) {
                List<PhotoItem> photoList = new ArrayList<>();
                try {
                    Document doc = Jsoup.connect(url).timeout(5000).get();
                    Element element = doc.select("div.wrap").last();
                    Elements items = element.select("div.kboxgrid");
                    for (Element ele : items) {
                        PhotoItem item = new PhotoItem();

                        Element content = ele.select("div.boxgrid").first();
                        Element info = ele.select("div.citemqt").first();

                        String name = info.select("a").first().text();
                        String imgUrl = content.select("img").first().attr("src");
                        String from =content.select("a").first().attr("href");
                        String date =info.select("a").last().text();

                        Log.d("prase", "*****from****"+from);
                        Log.d("prase", "*****date****" +date);
                        item.setName(name);
                        item.setImg(imgUrl);
                        item.setFrom(from);
                        item.setDate(date);

                        photoList.add(item);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return photoList;
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<List<PhotoItem>>() {
            @Override
            public void onCompleted() {

                binding.swipeRefreshLayout.setRefreshing(false);
                ((MainActivity) getActivity()).showSnack("加载完成");
            }

            @Override
            public void onError(Throwable e) {
                ((MainActivity) getActivity()).showSnack("加载失败");
                binding.swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onNext(List<PhotoItem> list) {

                adapter.setNewData(list);

            }
        });

    }


    /**
     * Recycler的分割线
     */
    private class SpacesItemDecoration extends RecyclerView.ItemDecoration {

        private int space;

        public SpacesItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.left = space;
            outRect.right = space;
            outRect.bottom = space;
            if (parent.getChildAdapterPosition(view) == 0) {
                outRect.top = space;
            }
        }
    }

    /**
     * 在Fragment销毁，把所有任务销毁掉
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(subscription!=null){
            subscription.unsubscribe();
        }
    }
}
