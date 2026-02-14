package stellar.snow.astralis.engine.render.hair;
import org.joml.*;
import java.util.*;
/**
 * Physics-based hair simulation using position-based dynamics
 * Simulates realistic hair movement with wind, gravity, and collisions
 */
    
    private static class HairStrand {
        Vector3f[] points;           // Current positions
        Vector3f[] prevPoints;       // Previous positions for Verlet
        Vector3f[] velocities;       // Velocities
        float[] lengths;             // Rest lengths between segments
        float[] masses;              // Mass per segment
        
        float stiffness = 0.8f;      // Bending resistance
        float damping = 0.95f;       // Energy loss
        float friction = 0.1f;       // Surface friction
        
        public HairStrand(int segments, Vector3f root, Vector3f direction, float length) {
            points = new Vector3f[segments];
            prevPoints = new Vector3f[segments];
            velocities = new Vector3f[segments];
            lengths = new float[segments - 1];
            masses = new float[segments];
            
            float segmentLength = length / segments;
            
            for (int i = 0; i < segments; i++) {
                // Initialize positions along direction
                Vector3f pos = new Vector3f(direction)
                    .mul(i * segmentLength)
                    .add(root);
                
                points[i] = new Vector3f(pos);
                prevPoints[i] = new Vector3f(pos);
                velocities[i] = new Vector3f();
                masses[i] = 0.01f;  // Small mass per segment
                
                if (i < segments - 1) {
                    lengths[i] = segmentLength;
                }
            }
        }
        
        public void integrate(float dt, Vector3f gravity, Vector3f wind) {
            // Verlet integration for each segment (except root which is fixed)
            for (int i = 1; i < points.length; i++) {
                Vector3f pos = points[i];
                Vector3f prev = prevPoints[i];
                
                // Calculate velocity using Verlet
                Vector3f velocity = new Vector3f(pos).sub(prev);
                
                // Apply forces
                Vector3f force = new Vector3f();
                
                // Gravity
                force.add(new Vector3f(gravity).mul(masses[i]));
                
                // Wind force (stronger at tips)
                float windStrength = (float)i / points.length;
                force.add(new Vector3f(wind).mul(windStrength * 0.5f));
                
                // Air resistance
                Vector3f drag = new Vector3f(velocity).mul(-0.1f);
                force.add(drag);
                
                // Verlet integration: x(t+dt) = 2x(t) - x(t-dt) + a*dt²
                Vector3f accel = new Vector3f(force).div(masses[i]);
                Vector3f newPos = new Vector3f(pos)
                    .mul(2.0f)
                    .sub(prev)
                    .add(new Vector3f(accel).mul(dt * dt));
                
                // Apply damping
                newPos.lerp(pos, 1.0f - damping);
                
                prevPoints[i].set(pos);
                points[i].set(newPos);
                
                // Update velocity for next frame
                velocities[i].set(velocity);
            }
        }
        
        public void applyConstraints(int iterations) {
            // Distance constraints to maintain hair length
            for (int iter = 0; iter < iterations; iter++) {
                // First point is fixed (root)
                for (int i = 0; i < points.length - 1; i++) {
                    Vector3f p1 = points[i];
                    Vector3f p2 = points[i + 1];
                    
                    Vector3f delta = new Vector3f(p2).sub(p1);
                    float distance = delta.length();
                    float restLength = lengths[i];
                    
                    if (distance > 0.0001f) {
                        float correction = (distance - restLength) / distance;
                        delta.mul(correction * 0.5f);
                        
                        // Don't move root
                        if (i > 0) {
                            p1.add(delta);
                        }
                        p2.sub(delta);
                    }
                }
                
                // Bending constraints (prevent sharp angles)
                for (int i = 0; i < points.length - 2; i++) {
                    Vector3f p0 = points[i];
                    Vector3f p1 = points[i + 1];
                    Vector3f p2 = points[i + 2];
                    
                    Vector3f dir1 = new Vector3f(p1).sub(p0).normalize();
                    Vector3f dir2 = new Vector3f(p2).sub(p1).normalize();
                    
                    // Target: maintain smooth curve
                    Vector3f targetDir = new Vector3f(dir1).add(dir2).normalize();
                    
                    // Apply correction
                    Vector3f correction = new Vector3f(targetDir).sub(dir2).mul(stiffness * 0.1f);
                    
                    if (i > 0) {
                        p1.add(new Vector3f(correction).mul(-0.5f));
                    }
                    p2.add(new Vector3f(correction).mul(0.5f));
                }
            }
        }
        
        public void collideWithSphere(Vector3f center, float radius) {
            // Collision with sphere (e.g., head)
            for (int i = 1; i < points.length; i++) {
                Vector3f p = points[i];
                Vector3f delta = new Vector3f(p).sub(center);
                float dist = delta.length();
                
                if (dist < radius) {
                    // Push point outside sphere
                    delta.normalize().mul(radius);
                    p.set(center).add(delta);
                    
                    // Apply friction
                    velocities[i].mul(1.0f - friction);
                }
            }
        }
    }
    
    private List<HairStrand> strands = new ArrayList<>();
    private Vector3f gravity = new Vector3f(0, -9.8f, 0);
    private Vector3f wind = new Vector3f(0, 0, 0);
    
    // Simulation parameters
    private int constraintIterations = 4;
    private float timeStep = 1.0f / 60.0f;
    private float timeAccumulator = 0.0f;
    
    // Collision objects
    private Vector3f headCenter = new Vector3f();
    private float headRadius = 10.0f;
    
    public void addStrand(Vector3f root, Vector3f direction, float length, int segments) {
        strands.add(new HairStrand(segments, root, direction, length));
    }
    
    public void simulate(float deltaTime, Vector3f gravity, Vector3f wind) {
        this.gravity.set(gravity);
        this.wind.set(wind);
        
        timeAccumulator += deltaTime;
        
        // Fixed time step simulation for stability
        while (timeAccumulator >= timeStep) {
            step(timeStep);
            timeAccumulator -= timeStep;
        }
    }
    
    private void step(float dt) {
        // Integrate all strands
        for (HairStrand strand : strands) {
            strand.integrate(dt, gravity, wind);
        }
        
        // Apply constraints
        for (HairStrand strand : strands) {
            strand.applyConstraints(constraintIterations);
        }
        
        // Handle collisions
        for (HairStrand strand : strands) {
            strand.collideWithSphere(headCenter, headRadius);
        }
    }
    
    public void setHeadCollision(Vector3f center, float radius) {
        this.headCenter.set(center);
        this.headRadius = radius;
    }
    
    public void setWind(Vector3f wind) {
        this.wind.set(wind);
    }
    
    public void setConstraintIterations(int iterations) {
        this.constraintIterations = Math.max(1, iterations);
    }
    
    public List<HairStrand> getStrands() {
        return strands;
    }
    
    public void reset() {
        strands.clear();
    }
}
