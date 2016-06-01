import java.io.*;
import java.lang.*;
import java.util.*;

// ルートの記憶変数をStringではなくlong型（各2bitで上下左右を表す）に変更する(最高で32手までに制限される)
// 要素の位置は9~14,17~22,25~30,33~38,41~49(((WIDTH+2)*height+1)~((WIDTH+2)*height)+WIDTH)となる。
// w  w  w  w  w  w  w   w
// w  9  10 11 12 13 14  w
// w  17 18 19 20 21 22  w
// w  25 26 27 28 29 30  w
// w  33 34 35 36 37 38  w
// w  41 42 43 44 45 46  w
// w  w  w  w  w  w  w   w
// 
// 全アクセス方法は、
// for(int i=1;i<=HEIGHT;i++){
// int start = (WIDTH+2)*i+1;
// int end = start+WIDTH;
// for(int j=start;j<end;j++){
// 	}
// }
//

/*
 * constructor(createCharList(),int)
 * main()
 * public static createCharlist()
 * setWall()
 * public getResult()
 * public start()
 * public setMaxDeep(int)
 * public setWidth(int)
 * public setHeight(int)
 * public getMaxCombo()
 * recSerch()
 * printSituation()
 * shiftRight()
 * shiftLeft()
 * shiftUp()
 * shiftDown()
 * dropAll()
 * drop()
 * countCombo()
 * checkDel()
 * judge()
 **/
public class PazDraRoot {
	private char[] currentDropList;
	private char[] movedDropList;
	private char[] resultDropList;
	private static char wall = 'w';
	private int maxCombo = Integer.MIN_VALUE;
	private int maxRootDeep = Integer.MIN_VALUE;
	private char[] maxRoot;
	private int maxFirstPos = 0;
	private int maxCurrentPos = 0;
	// states
	private int KIND_OF_DROP = 6;
	private static int WIDTH = 6;
	private static int HEIGHT = 5;
	private int maxDeep = 15;
	private int START_PRUNING = 10;

	// constructor
	private PazDraRoot(){
	}

	/**
	 * 新しいインスタンスを作ります。
	 * @param charArray ドロップ状態を規定の形式で表した文字配列です。通常、createCharList()を使用して作成します。
	 * @return 新しいPazDraRootインスタンスを返します。
	 */
	// static factory method
	public static PazDraRoot newInstance(char[] charArray) throws IllegalArgumentException,NullPointerException {
		return new PazDraRoot().setCurrentDropList(charArray);
	}
	private PazDraRoot setCurrentDropList(char[] charArray) throws IllegalArgumentException,NullPointerException {
		//currentDropList = charArray;
		//防御的コピー
		currentDropList = Arrays.copyOf(charArray,(WIDTH+2)*(HEIGHT+2));

		// 正当性検査
		for (int i=0; i<(WIDTH+2)*(HEIGHT+2); i++) {
			if (currentDropList[i] != 'w' && !(Character.isDigit(currentDropList[i])) && Character.digit(currentDropList[i],16) >= WIDTH ) {
				throw new IllegalArgumentException("use createCharList method at newInstance's augument'");
			}
		}
		return this;
	}

	// Driver
	public static void main(String[] arg){
		// 使用例
		try{
			// create instance
			// Arguments: first：staticメソッドcreateCharList()で作ったchar[]
			PazDraRoot test = PazDraRoot.newInstance(PazDraRoot.createCharList());

			Scanner sc = new Scanner(System.in);
			test.setMaxDeep(sc.nextInt());
			// start 
			test.start();
			// print default output
			test.getResult();
		}catch(IllegalArgumentException e){
			System.err.println("引数不正:" + e);
		}catch(NullPointerException e){
			System.err.println("引数不正:" + e);
		}catch(Exception e){
			System.err.println(e);
		}
	}
	/**
	 * 規定の形に沿ったドロップ状態を表す文字配列を作成します。このメソッドが実行されると、標準入力からドロップ状態をを数値で入力する必要があります。
	 * @return 規定の形に沿ったドロップ状態を表す文字配列を返します。
	 */
	public static char[] createCharList(){
		// Java入門練習問題11参照
		byte[] b = new byte[WIDTH*HEIGHT+1]; // 最後の改行分まで確保しなければ、readByte+readableで範囲を超えてしまい例外が起こる

		boolean reRead = false;
		do{
			if (reRead) {
				System.out.printf("再入力(横%d*縦%d)：\n",WIDTH,HEIGHT);
			}else{
				System.out.printf("ドロップの入力(%d*%d)：\n",WIDTH,HEIGHT);
			}
			reRead = false;
			int readByte = 0;
			try{
				int readable = WIDTH+1;
				do{
					System.in.skip((long)System.in.available()); // 読み込み前に余計な入力は破棄
					readByte += System.in.read(b,readByte,readable); // 改行を含めて読み込むので7byte。そうしなければ次の行を読み込むときに読み込んでいなかった改行を最初に読み込んでしまう
					// readByte -= readByte % WIDTH; // 改行含め超えた文の入力を取り消すため戻り
					readByte--;
					if (readByte+readable>WIDTH*HEIGHT) readable = (WIDTH*HEIGHT+1) - readByte;
				}while(readByte<WIDTH*HEIGHT);
			}catch(Exception e){
				System.err.printf(e + ":%d in createCharList().\n",readByte);
				for (int i=0; i < b.length; i++) {
					System.err.printf("%d:(%c),\n",i,(char)b[i]);
				}
				System.out.println("");
			}

			// 各値が正しいか確認
			for (int i=0; i<WIDTH*HEIGHT; i++) {
				if (!(Character.isDigit((char)b[i])) || Character.digit((char)b[i],10) >= WIDTH) {
					reRead = true;
				}
			}
		}while(reRead);


		return setWall(b);
	}

	private static char[] setWall(byte[] b){
		char[] c = new char[(WIDTH+2)*(HEIGHT+2)];
		// 外周に範囲外を示す文字'w'を設定
		int k = 0;
		for (int i=0; i<(WIDTH+2)*(HEIGHT+2); i++) {
			if (i<=(WIDTH+1) || i%(WIDTH+2)==0 || i%(WIDTH+2)==(WIDTH+1) || i>=(WIDTH+2)*(HEIGHT+1)) {
				c[i] = wall;
			}else{
				c[i] = (char)b[k++];
			}
		}
		b = null;
		return c;
	}

	/**
	 * ルート探索の結果を標準出力に出力します。
	 */
	public void getResult(){
		System.out.print("Root:");
		for (int i=0; i<maxRootDeep; i++) {

			System.out.print(maxRoot[i]);
		}
		System.out.println("");

		printSituation(currentDropList);
		System.out.println("firstPos:上から" + (maxFirstPos/(WIDTH+2)) + " 左から" + (maxFirstPos%(WIDTH+2)));
		System.out.println(maxRootDeep + "move!");
		printSituation(movedDropList);
		System.out.println("currentPos:上から" + (maxCurrentPos/(WIDTH+2)) + "左から" + (maxCurrentPos%(WIDTH+2)));
		System.out.println(maxCombo + "combo!!");
		printSituation(resultDropList);
	}
	/**
	 * ルート探索を開始します。
	 */
	public void start(){
		int current = 0;
		for(int i=1;i<=HEIGHT;i++){
			int start = (WIDTH+2)*i+1;
			int end = start+WIDTH;
			for(int j=start;j<end;j++){
				recSerch(j,j,currentDropList,new char[maxDeep],0,' ');

				current += 100.0d / (WIDTH*HEIGHT);
				System.out.print(current + "% ");
			}
		}
		System.out.print("100%\n");

		int pos = maxFirstPos;
		movedDropList = Arrays.copyOf(currentDropList,currentDropList.length);
		for(int i=0;i<maxRootDeep;i++){

			switch(maxRoot[i]){
				case 'r':
					pos = shiftRightEntity(pos,movedDropList);
					break;
				case 'l':
					pos = shiftLeftEntity(pos,movedDropList);
					break;
				case 'u':
					pos = shiftUpEntity(pos,movedDropList);
					break;
				case 'd':
					pos = shiftDownEntity(pos,movedDropList);
					break;
				default:
					break;
			}
		}
		maxCurrentPos = pos;
		resultDropList = Arrays.copyOf(movedDropList,movedDropList.length);
		countCombo(resultDropList);
	}

	/**
	 * 最大手数を設定します。設定しなければデフォルト値の15が使用されます。
	 * @param maxDeep 最大手数を表す数値です。
	 */
	public void setMaxDeep(int maxDeep){
		if ( maxDeep < 0) {
			throw new IllegalArgumentException();
		}
		this.maxDeep = maxDeep;
	}

	/**
	 * 横方向にあるドロップの数を設定します。設定しなければデフォルト値の6が使用されます。
	 * @param width 横方向にあるドロップの数です。
	 */
	public static void setWidth(int width){
		if ( width < 0) {
			throw new IllegalArgumentException();
		}
		WIDTH = width;
	}

	/**
	 * 縦方向にあるドロップの数を設定します。設定しなければデフォルト値の5が使用されます。
	 * @param height 縦方向にあるドロップの数です。
	 */
	public static void setHeight(int height){
		if ( height < 0) {
			throw new IllegalArgumentException();
		}
		HEIGHT = height;
	}

	/**
	 * 最大コンボ数を返します。この関数はstart()メソッドを利用した後に呼び出すようにしてください。
	 * @return start()メソッドをまだ利用していない時は-1を返します。そうでなければ最大コンボ数を返します。
	 */
	public int getMaxCombo(){
		if (maxCombo < 0) return -1;
		else return maxCombo;
	}


	private int recSerch(int firstPos,int pos,char[] dropList,char[] rooted,int deep,char chRoot){
		//	全ルートを探索する再帰関数
		//	 firstPos このルートの第一手がどこから始まったのかを示す位置番号です。
		//	 pos このルートがどこで終わっているのかを示す位置番号です。
		//	 dropList 現在のドロップ状況をWIDTH*HEIGHT(さらに外周をwallで囲んでいる)で表している文字配列です。
		//	 rooted 現在までの道筋を表す文字配列です。
		//	 deep 現在まで何手進んでいるかを表す数値です。
		//	 chRoot 最後に進んだ方向を表す文字です。
		//	 現在のルートで終わった場合のコンボ数を返します。
		if(dropList[pos] == wall) return 0;
		if(deep>maxDeep) return 0;
		char[] currentRoot = Arrays.copyOf(rooted,rooted.length);
		if(chRoot!=' ') currentRoot[deep-1] = chRoot;

		int count = countCombo(Arrays.copyOf(dropList,dropList.length));
		if(deep>START_PRUNING && count < maxCombo-2) return 0;
		if((count==maxCombo && deep < maxRootDeep)
				|| count > maxCombo ){
			maxFirstPos = firstPos;
			maxCombo = count;
			maxRoot = currentRoot;
			maxRootDeep = deep;
				}
		recSerch(firstPos,pos+1,shiftRight(pos,dropList),currentRoot,deep+1,'r');
		recSerch(firstPos,pos-1,shiftLeft(pos,dropList),currentRoot,deep+1,'l');
		recSerch(firstPos,pos-(WIDTH+2),shiftUp(pos,dropList),currentRoot,deep+1,'u');
		recSerch(firstPos,pos+(WIDTH+2),shiftDown(pos,dropList),currentRoot,deep+1,'d');

		return count;
	}

	void printSituation(char[] dropList){
		System.out.println("-------");
		for(int i=1;i<=HEIGHT;i++){
			int start = (WIDTH+2)*i+1;
			int end = start+WIDTH;
			for(int j=start;j<end;j++){
				System.out.print(dropList[j]);
			}
			System.out.println(' ');
		}
		System.out.println("-------");
	}
	// not only return result,but change list itself.
	private int shiftRightEntity(int pos,char[] dropList){
		if(dropList[pos+1]==wall) return pos;
		char c = dropList[pos];
		dropList[pos] = dropList[pos+1];
		dropList[pos+1] = c;
		return pos + 1;
	}
	private int shiftLeftEntity(int pos,char[] dropList){
		if(dropList[pos-1]==wall) return pos;
		char c = dropList[pos];
		dropList[pos] = dropList[pos-1];
		dropList[pos-1] = c;
		return pos -1;
	}
	private int shiftUpEntity(int pos,char[] dropList){
		if(dropList[pos-(WIDTH+2)]==wall) return pos;
		char c = dropList[pos];
		dropList[pos] = dropList[pos-(WIDTH+2)];
		dropList[pos-(WIDTH+2)] = c;
		return pos - (WIDTH+2);
	}
	private int shiftDownEntity(int pos,char[] dropList){
		if(dropList[pos+(WIDTH+2)]==wall) return pos;
		char c = dropList[pos];
		dropList[pos] = dropList[pos+(WIDTH+2)];
		dropList[pos+(WIDTH+2)] = c;
		return pos + (WIDTH+2);
	}
	// need only result
	private char[] shiftRight(int pos,char[] dropList){
		char[] charList = Arrays.copyOf(dropList,dropList.length);
		shiftRightEntity(pos,charList);
		return charList;
	}
	private char[] shiftLeft(int pos,char[] dropList){
		char[] charList = Arrays.copyOf(dropList,dropList.length);
		shiftLeftEntity(pos,charList);
		return charList;
	}
	private char[] shiftUp(int pos,char[] dropList){
		char[] charList = Arrays.copyOf(dropList,dropList.length);
		shiftUpEntity(pos,charList);
		return charList;
	}
	private char[] shiftDown(int pos,char[] dropList){
		char[] charList = Arrays.copyOf(dropList,dropList.length);
		shiftDownEntity(pos,charList);
		return charList;
	}

	private void dropAll(char[] dropList){
		for(int i=HEIGHT;i>=1;i--){
			int start = (WIDTH+2)*i;
			int end = start+WIDTH;
			for(int j=end;j>start;j--){
				drop(j,dropList);
			}
		}
	}
	// if pos's drop is 'f' or space,
	// upper and upper drop replaces pos's drop.
	// and then, space replaces upper replaced drop.
	// pos's drop -> upper drops -> first upper number drop
	// upper(and upper) drop -> space
	// second upper number drop -> unchangeable
	private void drop(int pos,char[] dropList){
		int i=pos-(WIDTH+2);
		while(dropList[i]!=wall &&
				(dropList[pos]=='f' || Character.isSpaceChar(dropList[pos]))){
			dropList[pos] = dropList[i];
			dropList[i] = ' ';
			i-=(WIDTH+2);
				}
		// if(i<0) str.replace(pos,pos+1,randumDrop());
	}
	// コンボ数を計算する
	// 一度消えるべきドロップを11から引くことで6~bの数値で置き換える
	// その後、6~bのドロップをfに置き換えて、削除するドロップの位置を確定する(6~bとすることで何のドロップが消えたのかも判別可能)
	// 二段階の評価となっている理由は、例えば
	// 013423
	// 005315
	// 034111
	// 345313
	// 530245
	// という並びになっていたとして、いきなり２行目最初の0でcheckDelを使ってしまうと２行目２列めの0まで削除してしまうため
	// この並びの場合は１列めの0三つのみ消えるのが正しい
	// また、縦横比較のみで削除ドロップを決めると、1のように十字型になっていたり変則的なつながり方で1コンボとなる形を複数コンボで数えてしまう
	private int countCombo(char[] dropList){
		// reverse 0~5 to 6~b
		judge(dropList);
		int count=0;
		// serch 6~b
		for(int i=1;i<=HEIGHT;i++){
			int start = (WIDTH+2)*i+1;
			int end = start+WIDTH;
			for(int j=start;j<end;j++){
				char color = dropList[j];
				if(color != ' '
						&&	Character.digit(color,16)>=KIND_OF_DROP
						&& Character.digit(color,16)<=KIND_OF_DROP*2-1){
					// 'f' replace block(same num)
					// num:attiribute
					// return count of delete same attribute
					checkDel(dropList,j,color);
					count++;
					// 消えたドロップの種類を集計するならここでcolorを集計
						}
			}
		}
		// delete 'f' and drops upper drop
		dropAll(dropList);
		// recursive call
		if(count==0) return 0;
		return countCombo(dropList)+count;
	}
	// 'f' replace adjoining same num. 
	// num = one of 6~b(attension number)
	private void checkDel(char[] dropList,int checkPos,char color){
		if (color == 'f') return;
		int[]	stack = new int[WIDTH*HEIGHT];
		int sp = 0;
		sp = checkAndStack(dropList,checkPos,color,stack,sp);
		while (sp > 0) {
			int pos = stack[sp];
			sp--;
			// right
			sp = checkAndStack(dropList,pos+1,color,stack,sp);
			// left
			sp = checkAndStack(dropList,pos-1,color,stack,sp);
			// up
			sp = checkAndStack(dropList,pos-(WIDTH+2),color,stack,sp);
			// down
			sp = checkAndStack(dropList,pos+(WIDTH+2),color,stack,sp);
		}
	}
	private int checkAndStack(char[] dropList,int checkPos,char color,int[] stack,int sp){
		if (dropList[checkPos] != color) return sp;

		dropList[checkPos] = 'f';
		sp++;
		stack[sp] = checkPos;
		return sp;
	}

	// 上下左右を比較して３つ以上揃っているドロップを6~bに変換
	// 発見してすぐに置き換えると３つ以上揃っている場合に４つ目以降を同色と認識してくれなくなるので、
	// 一度ArrayListにドロップ番号を入れておき、最後に置き換える
	private char[] judge(char[] dropList){
		List<Integer> repList = new ArrayList<Integer>();
		// horizon
		for(int i=1;i<=HEIGHT;i++){
			int start = (WIDTH+2)*i+1;
			int end = start+WIDTH;
			for(int j=start;j<end;j++){
				if(Character.isDigit(dropList[j])
						&& dropList[j]==dropList[j-1]
						&& dropList[j]==dropList[j+1]){
					repList.add(j-1);
					repList.add(j);
					repList.add(j+1);
						}
			}
		}
		// vertical
		for(int i=1;i<=HEIGHT;i++){
			int start = (WIDTH+2)*i+1;
			int end = start+WIDTH;
			for(int j=start;j<end;j++){
				if(Character.isDigit(dropList[j])
						&& dropList[j]==dropList[j+WIDTH+2]
						&& dropList[j+WIDTH+2]==dropList[j+(WIDTH+2)*2]){
					repList.add(j);
					repList.add(j+WIDTH+2);
					repList.add(j+(WIDTH+2)*2);
						}
			}
		}
		// replace
		for(int i=0;i<repList.size();i++){
			int pos = repList.get(i);
			char repNum = Character.forDigit((KIND_OF_DROP*2-1)-Character.digit(dropList[pos],16),16);
			if(Character.digit(dropList[pos],16)<KIND_OF_DROP) dropList[pos] = repNum;
		}
		return dropList;
	}
}
