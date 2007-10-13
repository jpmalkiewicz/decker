package decker.model;
import java.io.PrintStream;



/** this class implements the script command "for" */
final class ForLoopCommand extends Block
{
	private Expression variable, initial_value, condition, final_value, step;
	private ScriptNode increment;
	private boolean java_style; // true : for (i=0;i<=5;i++), false : for i = 0 to 5


	/** used for the syntax : for i = 0 to 5 step 2 */
	ForLoopCommand (final Expression _variable, final Expression _initial_value, final Expression _final_value, final Expression _step, final String _script_name, final int _script_line, final int _script_column) {
		super(_script_name, _script_line, _script_column);
		java_style = false;
		variable = _variable;
		initial_value = _initial_value;
		final_value = _final_value;
		step = _step;
	}


	/** used for the syntax : for (i=0; i<=5; i=i+2) */
	ForLoopCommand (final Expression _variable, final Expression _initial_value, final Expression _condition, final ScriptNode _increment, final String _script_name, final int _script_line, final int _script_column) {
		super(_script_name, _script_line, _script_column);
		java_style = true;
		variable = _variable;
		initial_value = _initial_value;
		condition = _condition;
		increment = _increment;
	}


	ForLoopCommand (final ForLoopCommand original)  {
		super(original);
		java_style = original.java_style;
		if (original.variable != null)
			variable = original.variable.copy();
		if (original.initial_value != null)
			initial_value = original.initial_value.copy();
		if (!original.java_style) {
			if (original.final_value != null)
				final_value = original.final_value.copy();
			if (original.step != null)
				step = original.step.copy();
		}
		else {
			if (original.condition != null)
				condition = original.condition.copy();
			if (original.increment != null)
				increment = original.increment.copy();
		}
	}


	public ScriptNode copy ()  {
		return new ForLoopCommand(this);
	}


	public Value execute ()  {
		if (java_style) { // Java style loop, e.g. for (i = 0; i <= 5; i++)
			// the variable section is optional, execute it if it exists
			if (variable != null) {
				Value v = AssignmentCommand.fetchOrCreateVariable(variable, true, this);
				if (initial_value != null) {
					v.setDirectly(initial_value.execute());
				}
			}
			// the condition section is optional too. if it is omitted, the loop will be infinite. use the break command to leave it then
			final Expression c = condition;
			final ScriptNode inc = increment;
			while (c == null || c.execute().equals(true)) {
				super.execute();
				// the increment section is optional
				if (inc != null) {
					inc.execute();
				}
			}
		}
		else { // BASIC style loop, e.g. for i = 0 to 5
			// create the loop variable and initialize it
			Value v = AssignmentCommand.fetchOrCreateVariable(variable, true, this);
			v.setDirectly(initial_value.execute());
			int vt = v.type();
			if (vt != Value.INTEGER && vt != Value.REAL) {
				throwException("the initial value of "+variable+" must be INTEGER or REAL, not "+v+" ("+v.typeName()+")");
			}
			// if we have a step variable, things are easy. otherwise always move towards the limit in steps of one
			if (step != null) {
				{
					// check whether the loop variable has passed the limit before the loop was even executed for the first time
					Value vstep = step.execute();
					int vst = vstep.type();
					if (vst != Value.INTEGER && vst != Value.REAL) {
						throwException("the step value of the loop must be INTEGER or REAL, not "+vstep+" ("+vstep.typeName()+")");
					}
					double vr = v.real();
					double vsr = vstep.real();
					Value vlimit = final_value.execute();
					final int vlt = vlimit.type();
					if (vlt != Value.INTEGER && vlt != Value.REAL) {
						throwException("the limiting value of the loop must be INTEGER or REAL, not "+vlimit+" ("+vlimit.typeName()+")");
					}
					double vlr = vlimit.real();
					if (( vlr < vr && vsr > 0 )||( vlr > vr && vsr < 0 ))
						return null;
				}
				while (true) {
					// the loop limit has not been reached yet. execute the loop body again
					super.execute();
					// if the loop variable is right on the limit, stop after one execution of the block
					vt = v.type();
					if (vt != Value.INTEGER && vt != Value.REAL) {
						throwException("when you change the for loop variable in the looped block, you must give it an INTEGER or REAL value, not "+v+" ("+v.typeName()+")");
					}
					Value vlimit = final_value.execute();
					int vlt = vlimit.type();
					if (vlt != Value.INTEGER && vlt != Value.REAL) {
						throwException("the limiting value of the loop must be INTEGER or REAL, not "+vlimit+" ("+vlimit.typeName()+")");
					}
					double vr = v.real();
					double vlr = vlimit.real();
					if (vr == vlr)
						break;
					// advance the loop variable one step
					Value vstep = step.execute();
					int vst = vstep.type();
					if (vst != Value.INTEGER && vst != Value.REAL) {
						throwException("the step value of the loop must be INTEGER or REAL, not "+vstep+" ("+vstep.typeName()+")");
					}
					double vsr = vstep.real();
					if (vt == Value.INTEGER && vst == Value.INTEGER) {
						v.set(v.integer()+vstep.integer());
					}
					else {
						v.set(vr+vsr);
					}
					vr += vsr;
					// stop here if the loop variable has passed the limit
					vlimit = final_value.execute();
					vlt = vlimit.type();
					if (vlt != Value.INTEGER && vlt != Value.REAL) {
						throwException("the limiting value of the loop must be INTEGER or REAL, not "+vlimit+" ("+vlimit.typeName()+")");
					}
					vlr = vlimit.real();
					if (( vlr < vr && vsr > 0 )||( vlr > vr && vsr < 0 ))
						break;
				}
			}
			else {
				while (true) {
					// execute the loop body
					super.execute();
					vt = v.type();
					if (vt != Value.INTEGER && vt != Value.REAL) {
						throwException("when you change the for loop variable in the looped block, you must give it an INTEGER or REAL value, not "+v+" ("+v.typeName()+")");
					}
					// move towards the limit and determine whether the loop needs to terminate
					Value vlimit = final_value.execute();
					int vlt = vlimit.type();
					if (vlt != Value.INTEGER && vlt != Value.REAL) {
						throwException("the limiting value of the loop must be INTEGER or REAL, not "+vlimit+" ("+vlimit.typeName()+")");
					}
					double vlr = vlimit.real();
					double vr = v.real();
					if (vlr == vr) // can't determine a direction. stop here
						break;
					// advance the loop variable
					if (vt == Value.INTEGER) {
						if (vlr > vr)
							v.set(v.integer()+1);
						else
							v.set(v.integer()-1);
					}
					else {
						if (vlr > vr)
							v.set(v.real()+1);
						else
							v.set(v.real()-1);
					}
					// break here if the variable has passed the limit
					if (( vlr > vr && vlr < vr+1 )||( vlr < vr && vlr > vr-1 ))
						break;
				}
			}
		}
		return null;
	}


	boolean print (final PrintStream out, final String indentation, boolean line_start)  {
		if (!line_start)
			out.println();
		if (java_style) {
			out.print(indentation+"for (");
			// print the variable section
			if (variable != null) {
				line_start = variable.print(out, indentation, false);
				if (initial_value != null) {
					out.print((line_start?indentation:" ") + "= ");
					line_start = initial_value.print(out, indentation, false);
				}
			}
			out.print((line_start?indentation:"") + "; ");
			// print the condition section
			if (condition != null) {
				line_start = condition.print(out, indentation, false);
			}
			out.print((line_start?indentation:"") + "; ");
			// print the section that advances the loop variable
			if (increment != null) {
				line_start = increment.print(out, indentation, false);
			}
			out.println((line_start?indentation:"") + ")");
		}
		else { // !java_style
			out.print(indentation+"for ");
			// print the variable section
			if (variable != null) {
				line_start = variable.print(out, indentation, false);
			}
			else {
				out.print("[variable not defined]");
				line_start = false;
			}
			out.print((line_start?indentation:" ") + "= ");
			if (initial_value != null) {
				line_start = initial_value.print(out, indentation, false);
			}
			else {
				out.print("[initial value not defined]");
				line_start = false;
			}
			out.print((line_start?indentation:" ") + "to ");
			// print the final value section
			if (final_value != null) {
				line_start = final_value.print(out, indentation, false);
			}
			else {
				out.print("[final value not defined]");
				line_start = false;
			}
			// print the section that advances the loop variable
			if (step != null) {
				out.print((line_start?indentation:" ") + "step ");
				line_start = step.print(out, indentation, false);
			}
			if (!line_start) {
				out.println();
			}
		}
		// print the block enclosed in the for loop
		return super.print(out, indentation, true);
	}


	public void replace (final String original, final boolean starts_with, final String replacement)  {
//		super.replace(original, starts_with, replacement);
//		conditional_expression.replace(original, starts_with, replacement);
	}
}