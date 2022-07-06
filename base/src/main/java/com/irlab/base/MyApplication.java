package com.irlab.base;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.room.Room;

import com.alibaba.android.arouter.launcher.ARouter;
import com.irlab.base.dao.SGFDAO;
import com.irlab.base.database.ConfigDatabase;
import com.irlab.base.database.SGFDatabase;
import com.irlab.base.database.UserDatabase;
import com.irlab.base.entity.SGF;
import com.irlab.base.entity.User;

public class MyApplication extends Application {
    public static final String TAG = "MyApplication";

    private boolean isDebugARouter = true;

    // 提供自己的唯一实例
    private static MyApplication MyApp;

    // 声明数据库对象
    private UserDatabase userDatabase;
    private SGFDatabase sgfDatabase;
    private ConfigDatabase configDatabase;

    // 声明公共的信息映射对象, 可当作全局变量使用 读内存比读磁盘快很多
    public SharedPreferences preferences;

    protected static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this.getApplicationContext();
        MyApp = this;
        if (isDebugARouter) {
            ARouter.openLog();
            ARouter.openDebug();
        }
        ARouter.init(MyApplication.this);
        // 建立数据库、初始化数据库对象
        userDatabase = UserDatabase.getInstance(context);
        sgfDatabase = SGFDatabase.getInstance(context);
        configDatabase = ConfigDatabase.getInstance(context);

        // 假数据测试
        SGFDAO sgfdao = getSgfDatabase().sgfDAO();
        sgfdao.deleteAll();
        SGF sgf = new SGF();
        sgf.setTitle("棋谱1");
        sgf.setDesc("这是一个棋谱描述");
        sgf.setResult("黑中盘胜");
        sgf.setCode("(;GM[1]AP[StoneBase:SGFParser.3.0.1]SZ[19]HA[0]RE[斯坦尼斯罗执黑 vs 马天放执白 黑胜5目半]\n" +
                ";B[pd];W[dd];B[pp];W[dq];B[cc];W[cd];B[dc];W[fc];B[ec];W[ed];B[fb];W[gc];B[gb];W[hc];B[dp];W[cp];B[co];W[ep];B[do];W[cq];B[dk];W[ci];B[di];W[dh];B[ei];W[cj];B[ck];W[nq];B[ch];W[cg];B[bh];W[bg];B[eh];W[dg];B[lq];W[no];B[pn];W[pr];B[qq];W[iq];B[gp];W[kp];B[ip];W[jp];B[io];W[hq];B[gq];W[eo];B[en];W[fn];B[fo];W[dn];B[em];W[fq];B[hr];W[ir];B[fr];W[hs];B[gr];W[cn];B[kr];W[lp];B[mq];W[oo];B[nr];W[po];B[qo];W[or];B[np];W[op];B[oq];W[pq];B[qp];W[nq];B[bo];W[mp];B[er];W[qf];B[qe];W[pf];B[nd];W[ql];B[qm];W[pl];B[nf];W[rr];B[qi];W[ng];B[og];W[ph];B[nh];W[pi];B[rl];W[rk];B[rm];W[rq];B[qj];W[qk];B[pj];W[oj];B[ok];W[om];B[qh];W[pg];B[nj];W[oi];B[pk];W[ni];B[mi];W[mh];B[nm];W[on];B[mj];W[oh];B[of];W[mf];B[me];W[lf];B[rf];W[pe];B[qd];W[rg];B[re];W[qg];B[rh];W[sg];B[oe];W[lh];B[kj];W[ld];B[jh];W[md];B[nc];W[jn];B[jl];W[im];B[jq];W[jr];B[kq];W[ks];B[ls];W[mr];B[js];W[is];B[lb];W[hb];B[jf];W[kc];B[kb];W[rj];B[hp];W[ks];B[sh];W[jc];B[js];W[bc];B[bb];W[ks];B[qr];W[qs];B[js];W[cb];B[db];W[ks];B[bd];W[be];B[js];W[el];B[fm];W[ks];B[rs];W[ss];B[js];W[fl];B[gm];W[il];B[gl];W[ij];B[ii];W[jj];B[ki];W[ks];B[ol];W[pm];B[js];W[hi];B[lm];W[ks];B[hh];W[ji];B[ih];W[jk];B[kk];W[hj];B[jm];W[in];B[js];W[kh];B[gh];W[li];B[ml];W[ks];B[bj];W[bi];B[dj];W[ah];B[js];W[fj];B[fk];W[ks];B[hk];W[ik];B[js];W[bn];B[eq];W[ks];B[bk];W[ad];B[ab];W[ek];B[dl];W[gk];B[js];W[bp];B[fp];W[ks];B[kn];W[jo];B[js];W[kl];B[km];W[ks];B[sf];W[nh];B[js];W[lj];B[lk];W[ks];B[ff];W[ga];B[fa];W[ha];B[ca];W[lr];B[ie];W[jb];B[ge];W[mb];B[mc];W[lc];B[nb];W[hn];B[ej];W[fk];B[ef];W[df];B[id];W[gn];B[ic];W[fd];B[ib];W[ia];B[kg];W[le];B[ne];W[fi];B[fh];W[mn];B[hl];W[aj];B[ak];W[lg];B[gi];W[gj];B[kf];W[ka];B[ma];W[eg];B[fg];W[hd];B[he];W[si];B[ee];W[ri];B[nl];W[de];B[ac];W[gs];B[fs];W[ja];B[la];W[ln];B[ko];W[lo];B[mm];W[bd];B[jd];W[ke];B[ai];W[br];B[cs];W[aj];B[je];W[hm];B[fe];W[go];B[ho];W[nn];B[gd];W[kd];B[tt];W[eo];B[dm];W[ai]C[鏉庢柊鑸燂細榛戞柟娉㈠叞Stanistaw Frejlak鍦ㄦ捣鍙傚创鍙傝禌锛岄┈澶╂斁绾夸笂姣旇禌??11pass涓�鎵嬨�傜\uE0C75??鏂\uE21A潶灏兼柉缃楀\uE1EE闃佃┕瀹滃吀锛岄┈澶╂斁瀵归樀闄堜箖鐢筹紝鏁\uE103\uE1EC鍏虫敞\n" +
                "])");
        sgfdao.insert(sgf);

        preferences = getSharedPreferences("config", Context.MODE_PRIVATE);
        Log.d(TAG, "onCreate");
    }

    // 提供获取自己实例的唯一方法
    public synchronized static MyApplication getInstance() {
        return MyApp;
    }

    // 提供获取数据库实例的方法
    public synchronized UserDatabase getUserDatabase() {
        return userDatabase;
    }

    public synchronized SGFDatabase getSgfDatabase() { return sgfDatabase; }

    public synchronized ConfigDatabase getConfigDatabase() { return configDatabase; }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public static Context getContext() {
        return context;
    }
}
