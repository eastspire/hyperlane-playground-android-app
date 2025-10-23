package com.example.demoapp.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.demoapp.fragments.ChatFragment;
import com.example.demoapp.fragments.ExploreFragment;
import com.example.demoapp.fragments.TraceFragment;
import com.example.demoapp.fragments.ProfileFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {
    
    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new ChatFragment();
            case 1:
                return new ExploreFragment();
            case 2:
                return new TraceFragment();
            case 3:
                return new ProfileFragment();
            default:
                return new ChatFragment();
        }
    }
    
    @Override
    public int getItemCount() {
        return 4;
    }
}
