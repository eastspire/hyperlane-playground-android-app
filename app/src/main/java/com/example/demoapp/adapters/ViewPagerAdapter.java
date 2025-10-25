package com.example.demoapp.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.demoapp.fragments.LogFragment;
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
                return new LogFragment();
            case 1:
                return new ChatFragment();
            case 2:
                return new ExploreFragment();
            case 3:
                return new TraceFragment();
            case 4:
                return new ProfileFragment();
            default:
                return new LogFragment();
        }
    }
    
    @Override
    public int getItemCount() {
        return 5;
    }
}
