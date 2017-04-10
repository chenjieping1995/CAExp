package exp02;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import java.awt.Font;
import java.awt.Color;
import javax.swing.SwingConstants;
import java.awt.Toolkit;
import javax.swing.ImageIcon;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class CCacheSim extends JFrame implements ActionListener{

	private JPanel panelTop, panelLeft, panelRight, panelBottom;
	private JButton execStepBtn, execAllBtn, fileBotton;
	private JComboBox csBox, bsBox, wayBox, replaceBox, prefetchBox, writeBox, allocBox;
	private JFileChooser fileChoose =new JFileChooser();
	
	private JLabel labelTop,labelLeft,rightLabel,bottomLabel,fileLabel,fileAddrBtn,
		    csLabel, bsLabel, wayLabel, replaceLabel, prefetchLabel, writeLabel, allocLabel;
	private JLabel results[];


    //参数定义
	private String cachesize[] = { "2KB", "8KB", "32KB", "128KB", "512KB", "2MB" };
	private String blocksize[] = { "16B", "32B", "64B", "128B", "256B" };
	private String way[] = { "直接映象", "2路", "4路", "8路", "16路", "32路" };
	private String replace[] = { "LRU", "FIFO", "RAND" };
	private String pref[] = { "不预取", "不命中预取" };
	private String write[] = { "写回法", "写直达法" };
	private String alloc[] = { "按写分配", "不按写分配" };
	private String typename[] = { "读数据", "写数据", "读指令" };
	private String hitname[] = {"不命中", "命中" };
	
	//右侧结果显示
	private String rightLable[]={"访问总次数：","读指令次数：","读数据次数：","写数据次数："};
	
	//打开文件
	private File file;
	
	//分别表示左侧几个下拉框所选择的第几项，索引从 0 开始
	private int csIndex, bsIndex, wayIndex, replaceIndex, prefetchIndex, writeIndex, allocIndex;
	
	//其它变量定义
	//...
	
	// 定义一个集合，存放指令和数据流
	ArrayList<String> content = new ArrayList<>();
	
	int size; // 总指令/数据流数
	int total; // 访问次数
	int totalHit;	// 命中次数
	int readIns; // 读指令次数
	int readInsHit; 
	int writeData ; // 写数据次数
	int writeDataHit;
	int readData; // 读数据次数
	int readDataHit;
	int index; // 当前指令的索引号
	
	int memWrite; // 写内存次数
	
	/**
	 * 声明一些显示可变结果数据的标签
	 */
	private JLabel resLabel[][];
	private JLabel addrInfo;
	private JLabel typeAndHit;
	private JLabel addrInfo2;
	
	/*
	 * 构造函数，绘制模拟器面板
	 */
	public CCacheSim(){
		super("Cache Simulator");
		setIconImage(Toolkit.getDefaultToolkit().getImage("D:\\2017spring\\计算机体系结构\\lab2\\图标.png"));
		setTitle("Cache模拟器");
		draw();
	}
	
	
	//响应事件，共有三种事件：
	//   1. 执行到底事件
	//   2. 单步执行事件
	//   3. 文件选择事件
	public void actionPerformed(ActionEvent e){
				
		if (e.getSource() == execAllBtn) {
			simExecAll();
		}
		if (e.getSource() == execStepBtn) {
			simExecStep(true);
		}
		if (e.getSource() == fileBotton){
			// 默认打开的目录为当前目录的父目录
			fileChoose.changeToParentDirectory();
			int fileOver = fileChoose.showOpenDialog(null);
			if (fileOver == 0) {
				String path = fileChoose.getSelectedFile().getAbsolutePath();
				fileAddrBtn.setText(path);
				file = new File(path);
				initCache();
				try {
					readFile();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}
	
	
	/**
	 * 一些简单的辅助函数
	 * @author CHEN
	 */
	// 幂运算
	private int pow(int x, int p) {
        return (int)Math.pow(x, p);
    }
 
	// 取2为底的对数值
    private int log2(int x) {
        return (int)(Math.log(x) / Math.log(2));
    }
 
    // 取[x,y)之间的一个随机数
    private int random(int x, int y) {
        return (int)Math.random() * (y - x) + x;
    }
    
    /**
     * 数据/指令流类
     * @author CHEN
     */
    private class Instruction{
    	int tag; // 标记域
    	int groupIndex; // Cache内组索引
    	int blockAddr; // 当前块全地址索引，在预取策略时使用过
    	int inBlockAddr; // 当前目标地址的块内地址
    	String address; // 目标内存地址
    	
    	/**
    	 * 构造函数
    	 * @param address：传入目标内存地址
    	 */
    	public Instruction(String address){
    		this.address = address;
    		String binAddr = this.hexToBinAddress();
    		// 获取地址串内的tag域
    		this.tag = Integer.parseInt(binAddr.substring(0, 32 - myCache.blockOffset - myCache.groupOffset), 2);
    		// 获取地址串内的组索引域
    		this.groupIndex = Integer.parseInt(binAddr.substring(32 - myCache.blockOffset - myCache.groupOffset, 32 - myCache.blockOffset), 2);
    		// 获取当前块的地址
    		this.blockAddr = Integer.parseInt(binAddr.substring(0, 32 - myCache.blockOffset), 2);
    		// 获取当前目标的块内地址
    		this.inBlockAddr = Integer.parseInt(binAddr.substring(32 - myCache.blockOffset), 2);
    	}
    	
    	/**
    	 * 将16进制的地址转换为32位二进制值返回
    	 * @return 32位的二进制串
    	 */
    	private String hexToBinAddress() {
            StringBuffer sb = new StringBuffer();
            int zero = 8 - this.address.length();
            for (int i = 0; i < zero; i++) {
                sb.append("0000");
            }
            int len = this.address.length();
            for (int i = 0; i < len; i++) {
                switch(this.address.charAt(i)) {
                    case '0':
                        sb.append("0000");
                        break;
                    case '1':
                        sb.append("0001");
                        break;
                    case '2':
                        sb.append("0010");
                        break;
                    case '3':
                        sb.append("0011");
                        break;
                    case '4':
                        sb.append("0100");
                        break;
                    case '5':
                        sb.append("0101");
                        break;
                    case '6':
                        sb.append("0110");
                        break;
                    case '7':
                        sb.append("0111");
                        break;
                    case '8':
                        sb.append("1000");
                        break;
                    case '9':
                        sb.append("1001");
                        break;
                    case 'a':
                        sb.append("1010");
                        break;
                    case 'b':
                        sb.append("1011");
                        break;
                    case 'c':
                        sb.append("1100");
                        break;
                    case 'd':
                        sb.append("1101");
                        break;
                    case 'e':
                        sb.append("1110");
                        break;
                    case 'f':
                        sb.append("1111");
                        break;
                    default:
                        System.out.println("Data Error!");
                        break;
                }
            }
            return sb.toString();
        }
    }


	/**
	 * 块信息类
	 * @author CHEN
	 */
	private class Block{
		int tag; // 标记域
		boolean isDirty; // 脏数据标记
		int count; // 访问次数
		long time;
		
		// 空参数的构造函数
		private Block(int tag) {
			this.tag = tag;
			isDirty = false;
			count = 0;
			time = -1L;
		}
	}

	/**
	 * 缓存类
	 * @author CHEN
	 */
	private class Cache {
		
		Block cache[][]; // 块记录数组
		int groupSize; // 每组内的块数
		int groupNum; // 分组数
		int groupOffset; // Cache内组偏移量
		int blockOffset; // 组内块偏移量
		int[] groupFIFOTime; // FIFO选择用的计数数组
		int[] groupLRUTime; // LRU选择用的计数数组
		
		// 构造函数
		public Cache(int cacheSize, int blockSize){
			groupSize = (int)pow(2, wayIndex);// 每组内的块数
			groupNum = cacheSize/(blockSize*groupSize); // 分组数
			groupOffset = log2(groupNum); // 组索引的位数
			blockOffset = log2(blockSize); // 组内块索引的位数
			
			cache = new Block [groupNum][groupSize];
			for (int i=0; i<groupNum; i++){
				for (int j=0; j<groupSize; j++){
					cache[i][j] = new Block(-1); // 新建块对象
				}
			}
			groupFIFOTime = new int[groupNum];
			groupLRUTime = new int[groupNum];
		}

		/**
		 * 模拟对Cache进行读操作
		 * @param tag 需要读的地址标记域
		 * @return true：读命中，false：未命中
		 */
		public boolean read(int tag, int groupIndex){
			for (int i=0; i<groupSize; i++){
				if (cache[groupIndex][i].tag==tag){
					// 目标内存的tag 与当前块的tag 相同，命中
					cache[groupIndex][i].count=groupLRUTime[groupIndex];
					groupLRUTime[groupIndex]++;
					return true;
				}
			}
			return false;
		}
		
		/**
		 * 模拟对Cache进行写操作
		 * @param tag 需要写的地址标记域
		 * @return true：块命中，false：未命中
		 */
		public boolean write(int tag, int groupIndex) {
			for (int i=0; i<groupSize; i++){
				if (cache[groupIndex][i].tag==tag){
					// 目标内存的tag 与当前块的tag 相同，命中
					cache[groupIndex][i].count=groupLRUTime[groupIndex];
					groupLRUTime[groupIndex]++;
					cache[groupIndex][i].isDirty = true;
					if (writeIndex==1){
						memWrite++;
						cache[groupIndex][i].isDirty = false;
					}
					return true;
				}
			}
			return false;
		}

		/**
		 * Block替换
		 * @param tag 需要被替换的block的标记域
		 * @param groupIndex 该块所在的分组
		 */
		public void replaceCacheBlock(int tag, int groupIndex) {
			if (replaceIndex == 0) {//LRU
                int lruBlock = 0;
                for (int i = 1; i < groupSize; i++) {
                    if (cache[groupIndex][lruBlock].count > cache[groupIndex][i].count) {
                        lruBlock = i;
                    } else if (cache[groupIndex][lruBlock].count==cache[groupIndex][i].count){
                    	// 当使用次数相同时，把最旧的换掉
                    	if (cache[groupIndex][lruBlock].time>cache[groupIndex][i].time)
                    		lruBlock = i;
                    }
                }
                loadToCache(tag, groupIndex, lruBlock);
            } else if (replaceIndex == 1) {//FIFO
                int fifoBlock = 0;
                for (int i = 1; i < groupSize; i++) {
                    if (cache[groupIndex][fifoBlock].time > cache[groupIndex][i].time) {
                        fifoBlock = i;
                    }
                }
                loadToCache(tag, groupIndex, fifoBlock);
            } else if (replaceIndex == 2) {//random
                int ranBlock = random(0, groupSize);
                loadToCache(tag, groupIndex, ranBlock);
            }
			
		}

		/**
		 * 将内存块载入到Cache
		 * @param tag：目标内存块的tag域
		 * @param groupIndex：目标在Cache中的组索引
		 * @param blockAddr：目标在Cache中的块索引 
		 */
		private void loadToCache(int tag, int groupIndex, int blockAddr) {
			cache[groupIndex][blockAddr].tag = tag;
			cache[groupIndex][blockAddr].count = groupLRUTime[groupIndex];
			groupLRUTime[groupIndex]++;
			cache[groupIndex][blockAddr].isDirty = false;
			cache[groupIndex][blockAddr].time = groupFIFOTime[groupIndex];
			groupFIFOTime[groupIndex]++;
		}
		
		/**
		 * 预取操作
		 * @param goalBlockAddr：要预取的数据所在块地址
		 */
		public void prefetch(int goalBlockAddr) {
            int nextTag = goalBlockAddr / pow(2, groupOffset + blockOffset);
            int nextIndex = goalBlockAddr / pow(2, blockOffset) % pow(2, groupOffset);
            replaceCacheBlock(nextTag, nextIndex);
        }
	}
	Cache myCache;
	
	/*
	 * 初始化 Cache 模拟器
	 */
	public void initCache() {
		// 数据初始化
		index = 0;
		totalHit = 0;
		readData = readDataHit = 0;
		readIns = readInsHit = 0;
		writeData = writeDataHit = 0;
		memWrite = 0;
		
		int cacheSize = (int) (2*1024*pow(4, csIndex)); // Cache总大小
		int blockSize = (int) (16*pow(2, bsIndex)); // 单位块大小
		
		myCache = new Cache(cacheSize, blockSize);
	}
	
	/*
	 * 将指令和数据流从文件中读入
	 */
	public void readFile() throws IOException {
		BufferedReader reader = null;
		reader = new BufferedReader(new FileReader(file));
		String string = null;
		while ((string = reader.readLine()) != null){
			content.add(string);
		}
		size = content.size(); // 总的 指令/数据流 数
		reader.close();
	}
	
	/*
	 * 模拟单步执行
	 * @param oneStepExec: 是否为单步执行(为更新UI而设计)
	 */
	public void simExecStep(boolean oneStepExec) {
		String nowStr = content.get(index);
		String[] strings = nowStr.split(" ");
		if (strings[1].contains("\t")){
			String[] s = strings[1].split("\t");
			strings[1] = s[0];
		}
		System.out.println(strings[0]+","+strings[1]);
		Instruction ins = new Instruction(strings[1]); // 初始化一条指令
		int tag = ins.tag;
		int groupIndex = ins.groupIndex;
		boolean isHit = false;
		switch (Integer.parseInt(strings[0])) {
		case 0: // 读数据操作
			readData++;
			isHit = myCache.read(tag, groupIndex);
			if (isHit){
				readDataHit++;
			} else { // 不命中时调入块到Cache
				myCache.replaceCacheBlock(tag, groupIndex);
			}
			break;
		case 1:	// 写数据操作
			writeData++;
			isHit = myCache.write(tag, groupIndex);
			if(isHit){
				writeDataHit++;
			} else { // 写未命中
                if (allocIndex == 0) {
                	// 按写分配，将目标块从内存中调入Cache
                    myCache.replaceCacheBlock(tag, groupIndex);
                    myCache.write(tag, groupIndex);
                } else if(allocIndex == 1){
                	// 不按写分配，写内存次数++
                	memWrite++;
                }
            }
			break;
		case 2: // 读指令操作
			readIns++;
			isHit = myCache.read(tag, groupIndex);
			if(isHit) {
				readInsHit++;
			} else {
				myCache.replaceCacheBlock(tag, groupIndex);
				if(prefetchIndex==0){ // 指令不预取数据
					// 无操作
				} else if (prefetchIndex == 1){ // 指令预取数据到Cache
					int goalBlockAddr = ins.blockAddr+1;
					myCache.prefetch(goalBlockAddr);
				}
			}
		default:
			break;
		}
		
		// 只有当是单步执行或者执行到底完成时，才更新UI
		if (oneStepExec || index==size-1){
			totalHit = readDataHit + readInsHit + writeDataHit;
			total = readData + readIns + writeData;
			updateUI(strings[0], ins, isHit);
		}
		index++; // 准备执行下一行
	}

	/**
	 * 更新UI操作，主要就是显示当前的各参数数据
	 */
	private void updateUI(String type, Instruction ins, boolean isHit) {
		// 总访问
		resLabel[0][0].setText(total+"");
		resLabel[0][1].setText(totalHit+"");
		resLabel[0][2].setText(String.format("%.2f", totalHit*100.0/total)+"%");
		
		// 读指令
		resLabel[1][0].setText(readIns+"");
		resLabel[1][1].setText(readInsHit+"");
		resLabel[1][2].setText(String.format("%.2f", readInsHit*100.0/readIns)+"%");
		
		// 读数据
		resLabel[2][0].setText(readData+"");
		resLabel[2][1].setText(readDataHit+"");
		resLabel[2][2].setText(String.format("%.2f", readDataHit*100.0/readData)+"%");
		
		// 写数据
		resLabel[3][0].setText(writeData+"");
		resLabel[3][1].setText(writeDataHit+"");
		resLabel[3][2].setText(String.format("%.2f", writeDataHit*100.0/writeData)+"%");
		
		// 访问类型及命中情况
		StringBuilder sb = new StringBuilder("访问类型：");
		switch (Integer.parseInt(type)) {
		case 0:
			sb.append("读数据");
			break;
		case 1:
			sb.append("写数据");
			break;
		case 2:
			sb.append("读指令");
			break;
		default:
			System.out.println("访问类型错误！");
			return;
		}
		sb.append("      命中情况：");
		if (isHit)	sb.append("命中");
		else sb.append("未命中");
		typeAndHit.setText(sb.toString());
		
		// 地址及块号
		StringBuilder sb1 = new StringBuilder("地址：");
		sb1.append(ins.address+"      ");
		sb1.append("块号：");
		sb1.append(ins.blockAddr+"");
		addrInfo.setText(sb1.toString());
		
		StringBuilder sb2 = new StringBuilder("组索引：");
		sb2.append(ins.groupIndex+"      ");
		sb2.append("块内地址：");
		sb2.append(ins.inBlockAddr+"");
		addrInfo2.setText(sb2.toString());
	}


	/*
	 * 模拟执行到底
	 * 当指令没有执行完成时，持续取指单步执行即可
	 */
	public void simExecAll() {
		while (index<size){
			simExecStep(false); // 添加参数以减少UI更新次数
		}
		/**
		 * 测试输出
		 */
//		System.out.println("读写次数："+total);
//		System.out.println("读写命中次数："+totalHit);
//		System.out.println("读写命中率："+totalHit*100.0/size+"%");
//		System.out.println("读数据命中："+readDataHit);
//		System.out.println("读指令命中："+readInsHit);
//		System.out.println("写数据命中："+writeDataHit);
	}

	
	public static void main(String[] args) {
		new CCacheSim();
	}
	
	/**
	 * 绘制 Cache 模拟器图形化界面
	 * 无需做修改
	 */
	public void draw() {
		//模拟器绘制面板
		getContentPane().setLayout(new BorderLayout(5,5));
		panelTop = new JPanel();
		panelTop.setBackground(Color.LIGHT_GRAY);
		panelLeft = new JPanel();
		panelLeft.setBackground(new Color(127, 255, 212));
		panelRight = new JPanel();
		panelBottom = new JPanel();
		panelBottom.setBackground(Color.LIGHT_GRAY);
		panelTop.setPreferredSize(new Dimension(800, 50));
		panelLeft.setPreferredSize(new Dimension(300, 450));
		panelRight.setPreferredSize(new Dimension(500, 450));
		panelBottom.setPreferredSize(new Dimension(800, 100));
		panelTop.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		panelLeft.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		panelRight.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		panelBottom.setBorder(new EtchedBorder(EtchedBorder.RAISED));

		//*****************************顶部面板绘制*****************************************//
		labelTop = new JLabel("MyCache—Cache模拟器");
		labelTop.setForeground(Color.BLACK);
		labelTop.setFont(new Font("楷体", Font.BOLD, 24));
		panelTop.add(labelTop);

		
		//*****************************左侧面板绘制*****************************************//
		labelLeft = new JLabel("设置参数");
		labelLeft.setForeground(Color.RED);
		labelLeft.setHorizontalAlignment(SwingConstants.CENTER);
		labelLeft.setFont(new Font("华文楷体", Font.PLAIN, 24));
		labelLeft.setPreferredSize(new Dimension(300, 40));
		
		//cache 大小设置
		csLabel = new JLabel("总大小：");
		csLabel.setHorizontalAlignment(SwingConstants.CENTER);
		csLabel.setPreferredSize(new Dimension(120, 30));
		csBox = new JComboBox(cachesize);
		csBox.setPreferredSize(new Dimension(160, 30));
		csBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				csIndex = csBox.getSelectedIndex();
			}
		});
		
		//cache 块大小设置
		bsLabel = new JLabel("块大小：");
		bsLabel.setHorizontalAlignment(SwingConstants.CENTER);
		bsLabel.setPreferredSize(new Dimension(120, 30));
		bsBox = new JComboBox(blocksize);
		bsBox.setPreferredSize(new Dimension(160, 30));
		bsBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				bsIndex = bsBox.getSelectedIndex();
			}
		});
		
		//相连度设置
		wayLabel = new JLabel("相联度：");
		wayLabel.setHorizontalAlignment(SwingConstants.CENTER);
		wayLabel.setPreferredSize(new Dimension(120, 30));
		wayBox = new JComboBox(way);
		wayBox.setPreferredSize(new Dimension(160, 30));
		wayBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				wayIndex = wayBox.getSelectedIndex();
			}
		});
		
		//替换策略设置
		replaceLabel = new JLabel("替换策略：");
		replaceLabel.setHorizontalAlignment(SwingConstants.CENTER);
		replaceLabel.setPreferredSize(new Dimension(120, 30));
		replaceBox = new JComboBox(replace);
		replaceBox.setPreferredSize(new Dimension(160, 30));
		replaceBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				replaceIndex = replaceBox.getSelectedIndex();
			}
		});
		
		//预取策略设置
		prefetchLabel = new JLabel("预取策略：");
		prefetchLabel.setHorizontalAlignment(SwingConstants.CENTER);
		prefetchLabel.setPreferredSize(new Dimension(120, 30));
		prefetchBox = new JComboBox(pref);
		prefetchBox.setPreferredSize(new Dimension(160, 30));
		prefetchBox.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				prefetchIndex = prefetchBox.getSelectedIndex();
			}
		});
		
		//写策略设置
		writeLabel = new JLabel("写策略：");
		writeLabel.setHorizontalAlignment(SwingConstants.CENTER);
		writeLabel.setPreferredSize(new Dimension(120, 30));
		writeBox = new JComboBox(write);
		writeBox.setPreferredSize(new Dimension(160, 30));
		writeBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				writeIndex = writeBox.getSelectedIndex();
			}
		});
		
		//调块策略
		allocLabel = new JLabel("写不命中调块策略：");
		allocLabel.setHorizontalAlignment(SwingConstants.CENTER);
		allocLabel.setPreferredSize(new Dimension(120, 30));
		allocBox = new JComboBox(alloc);
		allocBox.setPreferredSize(new Dimension(160, 30));
		allocBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				allocIndex = allocBox.getSelectedIndex();
			}
		});
		
		//选择指令流文件
		fileLabel = new JLabel("选择指令流文件：");
		fileLabel.setHorizontalAlignment(SwingConstants.CENTER);
		fileLabel.setPreferredSize(new Dimension(120, 30));
		fileAddrBtn = new JLabel();
		fileAddrBtn.setBackground(new Color(255, 255, 255));
		fileAddrBtn.setPreferredSize(new Dimension(210, 24));
		fileAddrBtn.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		fileBotton = new JButton("浏览");
		fileBotton.setFont(new Font("宋体", Font.PLAIN, 12));
		fileBotton.setPreferredSize(new Dimension(70,30));
		fileBotton.addActionListener(this);
		
		panelLeft.add(labelLeft);
		panelLeft.add(csLabel);
		panelLeft.add(csBox);
		panelLeft.add(bsLabel);
		panelLeft.add(bsBox);
		panelLeft.add(wayLabel);
		panelLeft.add(wayBox);
		panelLeft.add(replaceLabel);
		panelLeft.add(replaceBox);
		panelLeft.add(prefetchLabel);
		panelLeft.add(prefetchBox);
		panelLeft.add(writeLabel);
		panelLeft.add(writeBox);
		panelLeft.add(allocLabel);
		panelLeft.add(allocBox);
		panelLeft.add(fileLabel);
		panelLeft.add(fileAddrBtn);
		panelLeft.add(fileBotton);
		
		//*****************************右侧面板绘制*****************************************//
		//模拟结果展示区域
		rightLabel = new JLabel("模拟结果");
		rightLabel.setForeground(new Color(0, 0, 255));
		rightLabel.setBounds(0, 7, 500, 40);
		rightLabel.setFont(new Font("华文楷体", Font.PLAIN, 24));
		rightLabel.setHorizontalAlignment(SwingConstants.CENTER);
		rightLabel.setPreferredSize(new Dimension(500, 40));
		results = new JLabel[4];
		for (int i=0; i<4; i++) {
			results[i] = new JLabel("");
			results[i].setPreferredSize(new Dimension(500, 40));
		}
		panelRight.setLayout(null);
		
		panelRight.add(rightLabel);
		for (int i=0; i<4; i++) {
			panelRight.add(results[i]);
		}
		
		JLabel label_1 = new JLabel("命中次数：");
		label_1.setForeground(new Color(255, 69, 0));
		label_1.setFont(new Font("华文楷体", Font.PLAIN, 16));
		label_1.setBounds(192, 57, 80, 19);
		panelRight.add(label_1);
		
		JLabel label_2 = new JLabel("命中率：");
		label_2.setForeground(new Color(255, 69, 0));
		label_2.setFont(new Font("华文楷体", Font.PLAIN, 16));
		label_2.setBounds(339, 57, 64, 19);
		panelRight.add(label_2);
		
		JLabel label_3 = new JLabel("其中：");
		label_3.setFont(new Font("华文楷体", Font.PLAIN, 16));
		label_3.setBounds(30, 100, 54, 15);
		panelRight.add(label_3);
		
		JLabel label_4 = new JLabel("读指令次数：");
		label_4.setForeground(new Color(0, 0, 255));
		label_4.setFont(new Font("华文楷体", Font.PLAIN, 16));
		label_4.setBounds(30, 130, 96, 19);
		panelRight.add(label_4);
		
		JLabel label_5 = new JLabel("读数据次数：");
		label_5.setForeground(Color.BLUE);
		label_5.setFont(new Font("华文楷体", Font.PLAIN, 16));
		label_5.setBounds(30, 170, 96, 19);
		panelRight.add(label_5);
		
		JLabel label_6 = new JLabel("写数据次数：");
		label_6.setForeground(Color.BLUE);
		label_6.setFont(new Font("华文楷体", Font.PLAIN, 16));
		label_6.setBounds(30, 210, 96, 19);
		panelRight.add(label_6);
		
		JLabel label_7 = new JLabel("命中次数：");
		label_7.setForeground(new Color(0, 0, 255));
		label_7.setFont(new Font("华文楷体", Font.PLAIN, 16));
		label_7.setBounds(192, 130, 80, 19);
		panelRight.add(label_7);
		
		JLabel label_8 = new JLabel("命中次数：");
		label_8.setForeground(new Color(0, 0, 255));
		label_8.setFont(new Font("华文楷体", Font.PLAIN, 16));
		label_8.setBounds(192, 170, 80, 19);
		panelRight.add(label_8);
		
		JLabel label_9 = new JLabel("命中次数：");
		label_9.setForeground(new Color(0, 0, 255));
		label_9.setFont(new Font("华文楷体", Font.PLAIN, 16));
		label_9.setBounds(192, 210, 80, 19);
		panelRight.add(label_9);
		
		JLabel label_10 = new JLabel("命中率：");
		label_10.setForeground(new Color(0, 0, 255));
		label_10.setFont(new Font("华文楷体", Font.PLAIN, 16));
		label_10.setBounds(339, 130, 64, 19);
		panelRight.add(label_10);
		
		JLabel label11 = new JLabel("命中率：");
		label11.setForeground(new Color(0, 0, 255));
		label11.setFont(new Font("华文楷体", Font.PLAIN, 16));
		label11.setBounds(339, 170, 64, 19);
		panelRight.add(label11);
		
		JLabel label112 = new JLabel("命中率：");
		label112.setForeground(new Color(0, 0, 255));
		label112.setFont(new Font("华文楷体", Font.PLAIN, 16));
		label112.setBounds(339, 210, 64, 19);
		panelRight.add(label112);
		
		resLabel = new JLabel[4][3];
		
		resLabel[0][0] = new JLabel("0");
		resLabel[0][0].setForeground(new Color(255, 99, 71));
		resLabel[0][0].setFont(new Font("华文楷体", Font.PLAIN, 16));
		resLabel[0][0].setBounds(120, 57, 62, 19);
		panelRight.add(resLabel[0][0]);
		
		resLabel[0][1] = new JLabel("0");
		resLabel[0][1].setForeground(new Color(255, 99, 71));
		resLabel[0][1].setFont(new Font("华文楷体", Font.PLAIN, 16));
		resLabel[0][1].setBounds(266, 57, 63, 19);
		panelRight.add(resLabel[0][1]);
		
		resLabel[0][2] = new JLabel("0.00%");
		resLabel[0][2].setForeground(new Color(255, 99, 71));
		resLabel[0][2].setFont(new Font("华文楷体", Font.PLAIN, 16));
		resLabel[0][2].setBounds(396, 57, 62, 19);
		panelRight.add(resLabel[0][2]);
		
		resLabel[1][0] = new JLabel("0");
		resLabel[1][0].setForeground(new Color(0, 0, 255));
		resLabel[1][0].setFont(new Font("华文楷体", Font.PLAIN, 16));
		resLabel[1][0].setBounds(120, 130, 62, 19);
		panelRight.add(resLabel[1][0]);
		
		resLabel[2][0] = new JLabel("0");
		resLabel[2][0].setForeground(new Color(0, 0, 255));
		resLabel[2][0].setFont(new Font("华文楷体", Font.PLAIN, 16));
		resLabel[2][0].setBounds(120, 170, 62, 19);
		panelRight.add(resLabel[2][0]);
		
		resLabel[3][0] = new JLabel("0");
		resLabel[3][0].setForeground(new Color(0, 0, 255));
		resLabel[3][0].setFont(new Font("华文楷体", Font.PLAIN, 16));
		resLabel[3][0].setBounds(120, 210, 62, 19);
		panelRight.add(resLabel[3][0]);
		
		resLabel[3][1] = new JLabel("0");
		resLabel[3][1].setForeground(Color.BLUE);
		resLabel[3][1].setFont(new Font("华文楷体", Font.PLAIN, 16));
		resLabel[3][1].setBounds(266, 210, 63, 19);
		panelRight.add(resLabel[3][1]);
		
		resLabel[2][1] = new JLabel("0");
		resLabel[2][1].setForeground(Color.BLUE);
		resLabel[2][1].setFont(new Font("华文楷体", Font.PLAIN, 16));
		resLabel[2][1].setBounds(266, 170, 63, 19);
		panelRight.add(resLabel[2][1]);
		
		resLabel[1][1] = new JLabel("0");
		resLabel[1][1].setForeground(Color.BLUE);
		resLabel[1][1].setFont(new Font("华文楷体", Font.PLAIN, 16));
		resLabel[1][1].setBounds(266, 130, 63, 19);
		panelRight.add(resLabel[1][1]);
		
		resLabel[3][2] = new JLabel("0.00%");
		resLabel[3][2].setForeground(Color.BLUE);
		resLabel[3][2].setFont(new Font("华文楷体", Font.PLAIN, 16));
		resLabel[3][2].setBounds(396, 210, 62, 19);
		panelRight.add(resLabel[3][2]);
		
		resLabel[2][2] = new JLabel("0.00%");
		resLabel[2][2].setForeground(Color.BLUE);
		resLabel[2][2].setFont(new Font("华文楷体", Font.PLAIN, 16));
		resLabel[2][2].setBounds(396, 170, 62, 19);
		panelRight.add(resLabel[2][2]);
		
		resLabel[1][2] = new JLabel("0.00%");
		resLabel[1][2].setForeground(Color.BLUE);
		resLabel[1][2].setFont(new Font("华文楷体", Font.PLAIN, 16));
		resLabel[1][2].setBounds(396, 130, 62, 19);
		panelRight.add(resLabel[1][2]);
		
		/**
		 * 复位按钮，单击时，数据显示复位，初始化Cache
		 */
		JButton button = new JButton("复 位");
		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				for (int i=0; i<4; i++){
					for (int j=0; j<2; j++){
						resLabel[i][j].setText("0");
					}
				}
				for (int i=0; i<4; i++){
					resLabel[i][2].setText("0.00%");
				}
				addrInfo.setText("");
				addrInfo2.setText("");
				typeAndHit.setText("");
				initCache();
				content = null;
			}
		});
		button.setForeground(new Color(0, 0, 0));
		button.setFont(new Font("华文楷体", Font.PLAIN, 16));
		button.setBounds(339, 346, 93, 23);
		panelRight.add(button);
		getContentPane().add("South", panelBottom);
		setSize(820, 620);
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JLabel label = new JLabel("访问总次数：");
		label.setForeground(new Color(255, 69, 0));
		label.setBounds(30, 57, 96, 19);
		label.setVerticalAlignment(SwingConstants.BOTTOM);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setFont(new Font("华文楷体", Font.PLAIN, 16));
		panelRight.add(label);
		
		addrInfo = new JLabel("");
		addrInfo.setFont(new Font("华文楷体", Font.PLAIN, 16));
		addrInfo.setBounds(30, 278, 296, 19);
		panelRight.add(addrInfo);
		
		typeAndHit = new JLabel("");
		typeAndHit.setFont(new Font("华文楷体", Font.PLAIN, 16));
		typeAndHit.setBounds(30, 249, 296, 19);
		panelRight.add(typeAndHit);
		
		//*****************************底部面板绘制*****************************************//
		
		bottomLabel = new JLabel("执行控制");
		bottomLabel.setForeground(Color.RED);
		bottomLabel.setFont(new Font("华文楷体", Font.PLAIN, 18));
		bottomLabel.setHorizontalAlignment(SwingConstants.CENTER);
		bottomLabel.setBackground(Color.LIGHT_GRAY);
		bottomLabel.setPreferredSize(new Dimension(800, 30));
		execStepBtn = new JButton("单步执行");
		execStepBtn.setIcon(new ImageIcon("D:\\2017spring\\计算机体系结构\\lab2\\next16.png"));
		execStepBtn.setFont(new Font("华文楷体", Font.PLAIN, 16));
		execStepBtn.setLocation(100, 30);
		execStepBtn.addActionListener(this);
		execAllBtn = new JButton("执行到底");
		execAllBtn.setIcon(new ImageIcon("D:\\2017spring\\计算机体系结构\\lab2\\last16.png"));
		execAllBtn.setFont(new Font("华文楷体", Font.PLAIN, 16));
		execAllBtn.setLocation(300, 30);
		execAllBtn.addActionListener(this);
		
		panelBottom.add(bottomLabel);
		panelBottom.add(execStepBtn);
		panelBottom.add(execAllBtn);

		getContentPane().add("North", panelTop);
		getContentPane().add("West", panelLeft);
		getContentPane().add("Center", panelRight);
		
		addrInfo2 = new JLabel("");
		addrInfo2.setFont(new Font("华文楷体", Font.PLAIN, 16));
		addrInfo2.setBounds(30, 307, 296, 19);
		panelRight.add(addrInfo2);
	}
}

