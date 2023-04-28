package su.plo.lib.mod.client.gui.components;import su.plo.voice.universal.UDesktop;import su.plo.voice.universal.UGraphics;import su.plo.voice.universal.UKeyboard;import su.plo.voice.universal.UMatrixStack;import lombok.Getter;import lombok.Setter;import org.jetbrains.annotations.NotNull;import org.jetbrains.annotations.Nullable;import su.plo.lib.api.MathLib;import su.plo.lib.api.chat.MinecraftTextComponent;import su.plo.lib.mod.client.gui.narration.NarrationOutput;import su.plo.lib.mod.client.gui.widget.GuiAbstractWidget;import su.plo.lib.mod.client.render.RenderUtil;import java.util.Objects;import java.util.function.BiFunction;import java.util.function.Consumer;import java.util.function.Predicate;public class TextFieldWidget extends GuiAbstractWidget {    @Setter    @Nullable    private Consumer<String> responder;    @Setter    private Predicate<String> filter;    @Setter    private BiFunction<String, Integer, MinecraftTextComponent> formatter;    @Getter    private String value;    @Getter    private int maxLength;    private int frame;    @Getter    @Setter    private boolean editable;    @Getter    @Setter    private boolean bordered;    @Getter    @Setter    private boolean canLoseFocus;    private boolean shiftPressed;    private int displayPosition;    private int cursorPosition;    private int highlightPosition;    @Setter    private int textColor;    @Setter    private int textColorUneditable;    @Setter    @Nullable    private String suggestion;    public TextFieldWidget(int x,                           int y,                           int width,                           int height,                           @NotNull MinecraftTextComponent text) {        super(x, y, width, height, text);        this.value = "";        this.maxLength = 32;        this.bordered = true;        this.canLoseFocus = true;        this.editable = true;        this.textColor = 14737632;        this.textColorUneditable = 7368816;        this.filter = Objects::nonNull;        this.formatter = (string, integer) -> MinecraftTextComponent.literal(string);    }    @Override    public void updateNarration(@NotNull NarrationOutput narrationOutput) {        narrationOutput.add(NarrationOutput.Type.TITLE, MinecraftTextComponent.translatable("narration.edit_box", getValue()));    }    @Override    public boolean keyPressed(int keyCode, @Nullable UKeyboard.Modifiers modifiers) {        if (!canConsumeInput()) return false;        this.shiftPressed = UKeyboard.isShiftKeyDown();        if (UKeyboard.isKeyComboCtrlA(keyCode)) {            moveCursorToEnd();            setHighlightPos(0);            return true;        }        if (UKeyboard.isKeyComboCtrlC(keyCode)) {            UDesktop.setClipboardString(getHighlighted());            return true;        }        if (UKeyboard.isKeyComboCtrlV(keyCode)) {            if (isEditable()) {                insertText(UDesktop.getClipboardString());            }            return true;        }        if (UKeyboard.isKeyComboCtrlX(keyCode)) {            UDesktop.setClipboardString(getHighlighted());            if (isEditable()) {                insertText("");            }            return true;        }        switch (keyCode) {            case 259: // GLFW_KEY_BACKSPACE                if (isEditable()) {                    shiftPressed = false;                    deleteText(-1);                    shiftPressed = UKeyboard.isShiftKeyDown();                }                return true;            case 260: // GLFW_KEY_INSERT            case 264: // GLFW_KEY_DOWN            case 265: // GLFW_KEY_UP            case 266: // GLFW_KEY_PAGE_UP            case 267: // GLFW_KEY_PAGE_DOWN            default:                return false;            case 261: // GLFW_KEY_DELETE                if (isEditable()) {                    this.shiftPressed = false;                    this.deleteText(1);                    this.shiftPressed = UKeyboard.isShiftKeyDown();                }                return true;            case 262: // GLFW_KEY_RIGHT                if (UKeyboard.isCtrlKeyDown()) {                    moveCursorTo(getWordPosition(1));                } else {                    moveCursor(1);                }                return true;            case 263: // GLFW_KEY_LEFT                if (UKeyboard.isCtrlKeyDown()) {                    moveCursorTo(this.getWordPosition(-1));                } else {                    moveCursor(-1);                }                return true;            case 268: // GLFW_KEY_HOME                moveCursorToStart();                return true;            case 269: // GLFW_KEY_END                moveCursorToEnd();                return true;        }    }    @Override    public boolean charTyped(char typedChar, @Nullable UKeyboard.Modifiers modifiers) {        if (!canConsumeInput() || !isAllowedChatCharacter(typedChar)) return false;        if (isEditable()) insertText(Character.toString(typedChar));        return true;    }    @Override    public boolean mouseClicked(double mouseX, double mouseY, int button) {        if (!isVisible()) return false;        boolean mouseOver = isMouseOver(mouseX, mouseY);        if (canLoseFocus) setFocused(mouseOver);        if (isFocused() && mouseOver && isValidClickButton(button)) {            int j = MathLib.floor(mouseX) - x;            if (bordered) j -= 4;            String string = RenderUtil.stringToWidth(value.substring(displayPosition), getInnerWidth());            moveCursorTo(RenderUtil.stringToWidth(string, j).length() + displayPosition);            return true;        } else {            return false;        }    }    @Override    public boolean changeFocus(boolean lookForwards) {        return visible && isEditable() && super.changeFocus(lookForwards);    }    @Override    protected void onFocusedChanged(boolean focused) {        if (focused) this.frame = 0;    }    @Override    public boolean isMouseOver(double mouseX, double mouseY) {        return visible && super.isMouseOver(mouseX, mouseY);    }    @Override    public void renderButton(@NotNull UMatrixStack stack, int mouseX, int mouseY, float delta) {        if (!isVisible()) return;        int color;        if (isBordered()) {            color = isFocused() ? -1 : -6250336;            RenderUtil.fill(stack, x - 1, y - 1, x + width - 1, y + height - 1, color);            RenderUtil.fill(stack, x, y, x + width - 2, y + height - 2, -16777216);        }        color = isEditable() ? textColor : textColorUneditable;        int l = cursorPosition - displayPosition;        int m = highlightPosition - displayPosition;        String string = RenderUtil.stringToWidth(value.substring(displayPosition), getInnerWidth());        boolean bl = l >= 0 && l <= string.length();        boolean bl2 = isFocused() && frame / 6 % 2 == 0 && bl;        int n = bordered ? x + 4 : x;        int o = bordered ? y + (height - 10) / 2 : y;        int p = n;        if (m > string.length()) {            m = string.length();        }        if (!string.isEmpty()) {            String string2 = bl ? string.substring(0, l) : string;            p = RenderUtil.drawString(stack, formatter.apply(string2, displayPosition), n, o, color);        }        boolean bl3 = cursorPosition < value.length() || value.length() >= maxLength;        int q = p;        if (!bl) {            q = l > 0 ? n + width : n;        } else if (bl3) {            q = p - 1;            --p;        }        if (!string.isEmpty() && bl && l < string.length()) {            RenderUtil.drawString(stack, formatter.apply(string.substring(l), cursorPosition), p, o, color);        }        if (!bl3 && suggestion != null) {            RenderUtil.drawString(stack, suggestion, q - 1, o, -8355712);        }        if (string.isEmpty() && !isFocused()) {            RenderUtil.drawString(stack, getText(), n, o, -1);        }        int var10002;        int var10003;        int var10004;        if (bl2) {            if (bl3) {                var10002 = o - 1;                var10003 = q + 1;                var10004 = o + 1;                RenderUtil.fill(stack, q, var10002, var10003, var10004 + 9, -3092272);            } else {                RenderUtil.drawString(stack, "_", q, o, color);            }        }        if (m != l) {            int r = n + UGraphics.getStringWidth(string.substring(0, m));            var10002 = o - 1;            var10003 = r - 1;            var10004 = o + 1;            renderHighlight(stack, q, var10002, var10003, var10004 + 9);        }    }    private void renderHighlight(@NotNull UMatrixStack stack, int i, int j, int k, int l) {        int m;        if (i < k) {            m = i;            i = k;            k = m;        }        if (j < l) {            m = j;            j = l;            l = m;        }        if (k > x + width) {            k = x + width;        }        if (i > x + width) {            i = x + width;        }        UGraphics buffer = UGraphics.getFromTessellator();//        UGraphics.setShader(VertexBuilder.Shader.POSITION);        UGraphics.color4f(0F, 0F, 1F, 1F);//        render.disableTexture();        RenderUtil.enableColorLogic();        RenderUtil.logicOp("OR_REVERSE");        buffer.beginWithDefaultShader(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION);        buffer.pos(stack, i, l, 0D).endVertex();        buffer.pos(stack, k, l, 0D).endVertex();        buffer.pos(stack, k, j, 0D).endVertex();        buffer.pos(stack, i, j, 0D).endVertex();        buffer.drawDirect();        UGraphics.color4f(1F, 1F, 1F, 1F);        RenderUtil.disableColorLogic();//        render.enableTexture();    }    public boolean canConsumeInput() {        return isVisible() && isFocused() && isEditable();    }    public void tick() {        ++this.frame;    }    public void setValue(String value) {        if (!filter.test(value)) return;        if (value.length() > maxLength) {            this.value = value.substring(0, maxLength);        } else {            this.value = value;        }        this.moveCursorToEnd();        this.setHighlightPos(cursorPosition);        this.onValueChange(value);    }    public void setMaxLength(int maxLength) {        this.maxLength = maxLength;        if (value.length() > maxLength) {            this.value = value.substring(0, maxLength);            this.onValueChange(value);        }    }    public String getHighlighted() {        int start = Math.min(cursorPosition, highlightPosition);        int end = Math.max(cursorPosition, highlightPosition);        return value.substring(start, end);    }    public void insertText(String string) {        int i = Math.min(cursorPosition, highlightPosition);        int j = Math.max(cursorPosition, highlightPosition);        int k = maxLength - value.length() - (i - j);        String string2 = filterText(string);        int l = string2.length();        if (k < l) {            string2 = string2.substring(0, k);            l = k;        }        String string3 = (new StringBuilder(value)).replace(i, j, string2).toString();        if (filter.test(string3)) {            this.value = string3;            this.setCursorPosition(i + l);            this.setHighlightPos(cursorPosition);            this.onValueChange(value);        }    }    private void deleteText(int position) {        if (UKeyboard.isCtrlKeyDown()) {            deleteWords(position);        } else {            deleteChars(position);        }    }    public void deleteWords(int position) {        if (!value.isEmpty()) {            if (highlightPosition != cursorPosition) {                insertText("");            } else {                deleteChars(getWordPosition(position) - cursorPosition);            }        }    }    public void deleteChars(int i) {        if (!value.isEmpty()) {            if (highlightPosition != cursorPosition) {                this.insertText("");            } else {                int j = getCursorPos(i);                int k = Math.min(j, cursorPosition);                int l = Math.max(j, cursorPosition);                if (k != l) {                    String string = (new StringBuilder(value)).delete(k, l).toString();                    if (filter.test(string)) {                        this.value = string;                        this.moveCursorTo(k);                    }                }            }        }    }    public int getWordPosition(int position) {        return this.getWordPosition(position, cursorPosition);    }    private int getWordPosition(int position, int cursorPosition) {        return this.getWordPosition(position, cursorPosition, true);    }    private int getWordPosition(int i, int j, boolean bl) {        int k = j;        boolean bl2 = i < 0;        int l = Math.abs(i);        for (int m = 0; m < l; ++m) {            if (!bl2) {                int n = this.value.length();                k = this.value.indexOf(32, k);                if (k == -1) {                    k = n;                } else {                    while (bl && k < n && this.value.charAt(k) == ' ') {                        ++k;                    }                }            } else {                while (bl && k > 0 && this.value.charAt(k - 1) == ' ') {                    --k;                }                while (k > 0 && this.value.charAt(k - 1) != ' ') {                    --k;                }            }        }        return k;    }    public void setCursorPosition(int position) {        this.cursorPosition = MathLib.clamp(position, 0, value.length());    }    public void moveCursor(int position) {        moveCursorTo(getCursorPos(position));    }    private int getCursorPos(int position) {        return offsetByCodepoints(value, cursorPosition, position);    }    public int getInnerWidth() {        return isBordered() ? width - 8 : width;    }    public int getScreenX(int x) {        return x > value.length() ? this.x : this.x + UGraphics.getStringWidth(value.substring(0, x));    }    public void setX(int x) {        this.x = x;    }    public void setHighlightPos(int position) {        int valueLength = value.length();        this.highlightPosition = MathLib.clamp(position, 0, valueLength);        if (displayPosition > valueLength) {            this.displayPosition = valueLength;        }        int innerWidth = getInnerWidth();        String string = RenderUtil.stringToWidth(value.substring(displayPosition), innerWidth);        int l = string.length() + displayPosition;        if (highlightPosition == displayPosition) {            this.displayPosition -= RenderUtil.stringToWidth(value, innerWidth, true).length();        }        if (highlightPosition > l) {            this.displayPosition += highlightPosition - l;        } else if (highlightPosition <= displayPosition) {            this.displayPosition -= displayPosition - highlightPosition;        }        this.displayPosition = MathLib.clamp(displayPosition, 0, valueLength);    }    public void moveCursorToStart() {        moveCursorTo(0);    }    public void moveCursorToEnd() {        moveCursorTo(value.length());    }    public void moveCursorTo(int i) {        setCursorPosition(i);        if (!shiftPressed) {            setHighlightPos(cursorPosition);        }        onValueChange(value);    }    private void onValueChange(String string) {        if (responder != null) responder.accept(string);    }    @Override    protected MinecraftTextComponent createNarrationMessage() {        return MinecraftTextComponent.translatable("gui.narrate.editBox", getText(), value);    }    public static String filterText(String string) {        StringBuilder stringBuilder = new StringBuilder();        char[] var2 = string.toCharArray();        for (char c : var2) {            if (isAllowedChatCharacter(c)) {                stringBuilder.append(c);            }        }        return stringBuilder.toString();    }    public static boolean isAllowedChatCharacter(char c) {        return c != 167 && c >= ' ' && c != 127;    }    public static int offsetByCodepoints(String string, int i, int j) {        int k = string.length();        int l;        if (j >= 0) {            for (l = 0; i < k && l < j; ++l) {                if (Character.isHighSurrogate(string.charAt(i++)) && i < k && Character.isLowSurrogate(string.charAt(i))) {                    ++i;                }            }        } else {            for (l = j; i > 0 && l < 0; ++l) {                --i;                if (Character.isLowSurrogate(string.charAt(i)) && i > 0 && Character.isHighSurrogate(string.charAt(i - 1))) {                    --i;                }            }        }        return i;    }}